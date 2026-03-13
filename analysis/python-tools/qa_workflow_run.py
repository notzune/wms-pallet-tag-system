import argparse
import re
import time

from qa_telnet_probe import TelnetSession


def latest_text(session: TelnetSession) -> str:
    return session.text()


def wait_and_read(session: TelnetSession, seconds: float = 1.0) -> str:
    session.read_for(seconds)
    return latest_text(session)


def tail(text: str, lines: int = 35) -> str:
    return "\n".join(text.splitlines()[-lines:])


def type_text(session: TelnetSession, text: str, delay: float) -> None:
    session.send_text(text, delay=delay)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="10.19.96.122")
    parser.add_argument("--port", type=int, default=4550)
    parser.add_argument("--term", default="VMA493")
    parser.add_argument("--user", default="ZRASHED")
    parser.add_argument("--password", default="orange1")
    parser.add_argument("--loc", default="DOOR03")
    parser.add_argument("--vehicle", default="TRD2")
    parser.add_argument("--work-area", default="WA_MAIN")
    parser.add_argument("--delay", type=float, default=0.06)
    parser.add_argument("--max-safety", type=int, default=20)
    args = parser.parse_args()

    session = TelnetSession(args.host, args.port, "xterm")
    session.connect()
    try:
        wait_and_read(session, 1.2)
        type_text(session, args.term, args.delay)
        session.send_cr()
        text = wait_and_read(session, 1.5)

        if "Login" not in text:
            print(tail(text))
            return 2

        type_text(session, args.user, args.delay)
        session.send_tab()
        wait_and_read(session, 0.8)
        type_text(session, args.password, args.delay)
        session.send_enter()
        text = wait_and_read(session, 2.0)

        if "Work Information" not in text:
            print(tail(text))
            return 3

        type_text(session, args.loc, args.delay)
        session.send_tab()
        wait_and_read(session, 0.5)
        type_text(session, args.vehicle, args.delay)
        session.send_tab()
        wait_and_read(session, 0.5)
        type_text(session, args.work_area, args.delay)
        session.send_enter()
        text = wait_and_read(session, 2.0)

        if "Confirm Workflow" not in text:
            print(tail(text))
            return 4

        session.send_enter()
        text = wait_and_read(session, 1.5)

        count = 0
        while "Confirm: (Y|N):" in text and count < args.max_safety:
            session.send_text("Y", delay=args.delay)
            text = wait_and_read(session, 1.0)
            count += 1

        print(tail(text, 60))
        return 0
    finally:
        session.close()


if __name__ == "__main__":
    raise SystemExit(main())
