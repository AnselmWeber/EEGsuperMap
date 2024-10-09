import argparse
import time
import brainflow
import numpy as np
import random
import threading

from brainflow.board_shim import (
    BoardShim,
    BrainFlowInputParams,
    BoardIds,
    BrainFlowPresets,
)
from brainflow.data_filter import DataFilter, FilterTypes, DetrendOperations
from pythonosc import udp_client
from pythonosc.dispatcher import Dispatcher
from pythonosc import osc_server

read = True

switch = False
switch_lock = threading.Lock()

write = False
filename = "haha"
write_lock = threading.Lock()


def update_write_value(addr, args):
    global write
    global filename
    with write_lock: 
        write = not write
        print(f"writing file as {args} ... {write}")
        filename = str(args) + ".txt"

def update_switch_value(new_switch):
    global switch

    datamode ="board"

    if new_switch: 
        datamode = "file"

    with switch_lock:
        print(f'setting datamode to {datamode}')
        switch = new_switch
    
def update_write_handler(addr, args):
    update_write_value(not write, args)


def update_switch_handler(addr, args):
    update_switch_value(not switch)


def start_server(server):
    print("Serving on {}".format(server.server_address))
    server.serve_forever()

def main():
    global shared_value
    global read
    global filename

    BoardShim.enable_dev_board_logger()
    parser = argparse.ArgumentParser()
    # use docs to check which parameters are required for specific board, e.g. for Cyton - set serial port
    parser.add_argument(
        "--timeout",
        type=int,
        help="timeout for device discovery or connection",
        required=False,
        default=0,
    )
    parser.add_argument(
        "--ip-port", type=int, help="ip port", required=False, default=0
    )
    parser.add_argument(
        "--ip-protocol",
        type=int,
        help="ip protocol, check IpProtocolType enum",
        required=False,
        default=0,
    )
    parser.add_argument(
        "--ip-address", type=str, help="ip address", required=False, default=""
    )
    parser.add_argument(
        "--serial-port",
        type=str,
        help="serial port",
        required=False,
        default="/dev/cu.usbmodem11",
    )
    parser.add_argument(
        "--mac-address", type=str, help="mac address", required=False, default=""
    )
    parser.add_argument(
        "--other-info", type=str, help="other info", required=False, default=""
    )
    parser.add_argument(
        "--serial-number", type=str, help="serial number", required=False, default=""
    )
    parser.add_argument(
        "--board-id",
        type=int,
        help="board id, check docs to get a list of supported boards",
        required=False,
        default=BoardIds.SYNTHETIC_BOARD,
    )
    parser.add_argument("--file", type=str, help="file", required=False, default="")
    parser.add_argument(
        "--master-board",
        type=int,
        help="master board id for streaming and playback boards",
        required=False,
        default=BoardIds.NO_BOARD,
    )
    args = parser.parse_args()

    params = BrainFlowInputParams()
    params.ip_port = args.ip_port
    params.serial_port = args.serial_port
    params.mac_address = args.mac_address
    params.other_info = args.other_info
    params.serial_number = args.serial_number
    params.ip_address = args.ip_address
    params.ip_protocol = args.ip_protocol
    params.timeout = args.timeout
    params.file = args.file
    params.master_board = args.master_board

    board = BoardShim(args.board_id, params)
    board.prepare_session()

    board.start_stream()
    osc_parser = argparse.ArgumentParser()
    print(board.get_sampling_rate(board.board_id))

    osc_parser.add_argument(
        "--ip", default="127.0.0.1", help="The ip of the OSC server"
    )
    osc_parser.add_argument(
        "--port",
        type=int,
        default=57120,
        help="The port the OSC server is listening on",
    )
    osc_args = osc_parser.parse_args()

    update_port = 5555
    sc_port = 57120
    client = udp_client.SimpleUDPClient(osc_args.ip, sc_port)

    sr = board.get_sampling_rate(board.board_id)
    client.send_message("/board/sr", sr)
    count = 0

    # print(header)

    filename = str(filename) + ".txt"
    # DataFilter.write_file(header, filename, 'w')
    """
    with open(filename, "w") as text_file:
        text_file.write(header)
    """

    # Globale Variable zum Speichern des Werts
    print("before server")
    dispatcher = Dispatcher()

    dispatcher.map("/py/switch", update_switch_handler)
    dispatcher.map("/py/write", update_write_handler)


    server = osc_server.ThreadingOSCUDPServer((osc_args.ip, update_port), dispatcher)
    chunk_len = 10
    try:
        data = DataFilter.read_file(file_name=filename)
    except Exception:
        data = np.zeros(chunk_len)

    # server.serve_forever()
    print("before server thread")
    server_thread = threading.Thread(target=start_server, args=(server,))
    server_thread.start()
    print("after server thread")
   
    data_len = len(data.transpose())
    count = 0
    

    def process_chunk(chunk, filename):
        for channel in range(1, 5):     
            DataFilter.detrend(chunk[channel], DetrendOperations.CONSTANT.value)
            client.send_message(f"/data/{channel}", chunk[channel])
            
   
    try:
        while True:
            with switch_lock:
                current_switch = switch

            if current_switch:
                end = count + chunk_len
                chunk = data.transpose()[count:end].transpose()
                count = (count + chunk_len) % (data_len - chunk_len)
            else:
                chunk = board.get_current_board_data(10)
                with write_lock:
                    if write_lock:
                        DataFilter.write_file(chunk, filename, "a")  # use 'a' for append mode

            process_chunk(chunk, filename)
            time.sleep(1 / sr)

            with switch_lock:
                if switch != current_switch:
                    continue


    except KeyboardInterrupt:
        print("stop and release")
        board.stop_stream()
        board.release_session()


if __name__ == "__main__":
    main()
