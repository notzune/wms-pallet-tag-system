import argparse
from dataclasses import dataclass

from qa_telnet_probe import TelnetSession


@dataclass
class PickupMove:
    trailer: str
    carrier: str
    source: str
    deposit_to: str


SAFETY_Y_MAX = 20


def type_text(session: TelnetSession, value: str, delay: float) -> None:
    session.send_text(value, delay=delay)


def read(session: TelnetSession, seconds: float = 1.0) -> str:
    return session.read_for(seconds)


def bootstrap_to_shipping(
    session: TelnetSession,
    term: str,
    user: str,
    password: str,
    loc: str,
    vehicle: str,
    work_area: str,
    delay: float,
) -> str:
    read(session, 1.2)
    type_text(session, term, delay)
    session.send_cr()
    text = read(session, 1.5)
    if "Login" not in text:
        return text

    type_text(session, user, delay)
    session.send_tab()
    read(session, 0.6)
    type_text(session, password, delay)
    session.send_enter()
    text = read(session, 2.0)
    if "Work Information" not in text:
        return text

    type_text(session, loc, delay)
    session.send_tab()
    read(session, 0.4)
    type_text(session, vehicle, delay)
    session.send_tab()
    read(session, 0.4)
    type_text(session, work_area, delay)
    session.send_enter()
    text = read(session, 1.8)
    if "Confirm Workflow" not in text:
        return text

    session.send_enter()
    text = read(session, 1.0)
    count = 0
    while "Undirected Menu" not in text and count < SAFETY_Y_MAX:
        if "Confirm: (Y|N):" in text:
            session.send_text("Y", delay=delay)
            text = read(session, 0.9)
            count += 1
            continue
        text = read(session, 1.2)
    if "Undirected Menu" not in text:
        return text

    session.send_text("4", delay=delay)
    session.send_enter()
    text = read(session, 1.5)
    if "Shipping Menu" not in text:
        text = read(session, 1.5)
    if "Shipping Menu" not in text:
        return text

    session.send_text("4", delay=delay)
    session.send_enter()
    return read(session, 1.5)


def do_pickup(session: TelnetSession, move: PickupMove, delay: float) -> str:
    text = read(session, 0.4)
    if "Trailer Pickup" not in text:
        text = session.text()

    type_text(session, move.trailer, delay)
    session.send_tab()
    read(session, 0.4)
    type_text(session, move.carrier, delay)
    session.send_tab()
    read(session, 0.4)
    type_text(session, move.source, delay)
    session.send_enter()
    text = read(session, 1.8)
    if "Ok to move trailer? (Y|N):" not in text:
        return text

    session.send_text("Y", delay=delay)
    text = read(session, 1.5)
    if "Trailer Deposit" not in text:
        return text

    type_text(session, move.deposit_to, delay)
    session.send_tab()
    text = read(session, 0.7)
    if "OK To Begin Activity? (Y|N):" not in text:
        return text

    session.send_text("Y", delay=delay)
    text = read(session, 1.4)
    if "Trailer Deposited - Press Enter" not in text:
        return text

    session.send_enter()
    text = read(session, 1.0)
    return text


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="10.19.96.122")
    parser.add_argument("--port", type=int, default=4550)
    parser.add_argument("--term", default="VMA478")
    parser.add_argument("--user", default="ZRASHED")
    parser.add_argument("--password", default="orange1")
    parser.add_argument("--loc", default="DOOR03")
    parser.add_argument("--vehicle", default="TRD2")
    parser.add_argument("--work-area", default="WA_MAIN")
    parser.add_argument("--delay", type=float, default=0.06)
    parser.add_argument(
        "--move",
        action="append",
        required=True,
        help="trailer,carrier,source,deposit_to",
    )
    args = parser.parse_args()

    moves = [PickupMove(*item.split(",")) for item in args.move]
    session = TelnetSession(args.host, args.port, "xterm")
    session.connect()
    try:
        text = bootstrap_to_shipping(
            session,
            args.term,
            args.user,
            args.password,
            args.loc,
            args.vehicle,
            args.work_area,
            args.delay,
        )
        if "Trailer Pickup" not in text:
            print(text)
            return 2

        for move in moves:
            text = do_pickup(session, move, args.delay)
            if "Trailer Pickup" not in text:
                print(f"FAILED:{move.trailer}")
                print(text)
                return 3
            print(f"MOVED:{move.trailer}:{move.source}->{move.deposit_to}")

        print(session.tail(60))
        return 0
    finally:
        session.close()


if __name__ == "__main__":
    raise SystemExit(main())
