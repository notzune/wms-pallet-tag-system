import argparse
import re
import socket
import sys
import time


IAC = 255
DO = 253
DONT = 254
WILL = 251
WONT = 252
SB = 250
SE = 240
TTYPE = 24
NAWS = 31
ECHO = 1
SGA = 3


ANSI_RE = re.compile(r"\x1b\[[0-9;?]*[ -/]*[@-~]")


class TelnetSession:
    def __init__(self, host: str, port: int, term_type: str, width: int = 80, height: int = 24):
        self.host = host
        self.port = port
        self.term_type = term_type.encode("ascii")
        self.width = width
        self.height = height
        self.sock: socket.socket | None = None
        self.buffer = bytearray()

    def connect(self) -> None:
        self.sock = socket.create_connection((self.host, self.port), timeout=10)
        self.sock.settimeout(0.5)
        self._negotiate_for(1.5)

    def close(self) -> None:
        if self.sock is not None:
            try:
                self.sock.close()
            finally:
                self.sock = None

    def _send(self, data: bytes) -> None:
        assert self.sock is not None
        self.sock.sendall(data)

    def _send_cmd(self, cmd: int, opt: int) -> None:
        self._send(bytes([IAC, cmd, opt]))

    def _send_naws(self) -> None:
        width = self.width.to_bytes(2, "big")
        height = self.height.to_bytes(2, "big")
        self._send(bytes([IAC, SB, NAWS]) + width + height + bytes([IAC, SE]))

    def _send_ttype(self) -> None:
        self._send(bytes([IAC, SB, TTYPE, 0]) + self.term_type + bytes([IAC, SE]))

    def _handle_iac(self, data: bytes, idx: int) -> int:
        if idx + 1 >= len(data):
            return len(data)
        cmd = data[idx + 1]
        if cmd in (DO, DONT, WILL, WONT):
            if idx + 2 >= len(data):
                return len(data)
            opt = data[idx + 2]
            if cmd == DO:
                if opt in (TTYPE, NAWS, SGA):
                    self._send_cmd(WILL, opt)
                    if opt == NAWS:
                        self._send_naws()
                else:
                    self._send_cmd(WONT, opt)
            elif cmd == WILL:
                if opt in (ECHO, SGA):
                    self._send_cmd(DO, opt)
                else:
                    self._send_cmd(DONT, opt)
            elif cmd == DONT:
                self._send_cmd(WONT, opt)
            elif cmd == WONT:
                self._send_cmd(DONT, opt)
            return idx + 3
        if cmd == SB:
            end = data.find(bytes([IAC, SE]), idx + 2)
            if end == -1:
                return len(data)
            payload = data[idx + 2:end]
            if payload[:2] == bytes([TTYPE, 1]):
                self._send_ttype()
            return end + 2
        return idx + 2

    def _consume(self, data: bytes) -> None:
        idx = 0
        while idx < len(data):
            if data[idx] == IAC:
                idx = self._handle_iac(data, idx)
            else:
                self.buffer.append(data[idx])
                idx += 1

    def _negotiate_for(self, seconds: float) -> None:
        end = time.time() + seconds
        while time.time() < end:
            try:
                chunk = self.sock.recv(4096)
            except TimeoutError:
                continue
            except socket.timeout:
                continue
            if not chunk:
                break
            self._consume(chunk)

    def read_for(self, seconds: float) -> str:
        end = time.time() + seconds
        while time.time() < end:
            try:
                chunk = self.sock.recv(4096)
            except TimeoutError:
                continue
            except socket.timeout:
                continue
            if not chunk:
                break
            self._consume(chunk)
        return self.text()

    def send_text(self, text: str, delay: float = 0.03) -> None:
        for ch in text.encode("ascii", errors="ignore"):
            self._send(bytes([ch]))
            time.sleep(delay)

    def send_cr(self) -> None:
        self._send(b"\r")

    def send_enter(self) -> None:
        self._send(b"\r\n")

    def send_tab(self) -> None:
        self._send(b"\t")

    def send_pf1(self) -> None:
        self._send(b"\x1bOP")

    def send_raw(self, payload: bytes) -> None:
        self._send(payload)

    def text(self) -> str:
        raw = self.buffer.decode("latin1", errors="ignore")
        clean = ANSI_RE.sub("", raw)
        clean = clean.replace("\r", "\n")
        clean = clean.replace("\x00", "")
        clean = clean.replace("\x07", "")
        return clean

    def tail(self, lines: int = 40) -> str:
        text = self.text()
        return "\n".join(text.splitlines()[-lines:])

    def clear_buffer(self) -> None:
        self.buffer.clear()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="10.19.96.122")
    parser.add_argument("--port", type=int, default=4550)
    parser.add_argument("--term-type", default="xterm")
    parser.add_argument("--delay", type=float, default=0.05)
    parser.add_argument("--read", type=float, default=0.8)
    parser.add_argument("actions", nargs="*")
    args = parser.parse_args()

    session = TelnetSession(args.host, args.port, args.term_type)
    session.connect()
    try:
        session.read_for(args.read)
        for action in args.actions:
            if action == "RESETBUF":
                session.clear_buffer()
            elif action.startswith("WAIT:"):
                time.sleep(float(action.split(":", 1)[1]))
            elif action == "CR":
                session.send_cr()
            elif action == "ENTER":
                session.send_enter()
            elif action == "TAB":
                session.send_tab()
            elif action == "PF1":
                session.send_pf1()
            elif action.startswith("RAW:"):
                session.send_raw(bytes.fromhex(action[4:]))
            else:
                session.send_text(action, delay=args.delay)
            session.read_for(args.read)
        sys.stdout.write(session.tail())
        if not session.tail().endswith("\n"):
            sys.stdout.write("\n")
        return 0
    finally:
        session.close()


if __name__ == "__main__":
    raise SystemExit(main())
