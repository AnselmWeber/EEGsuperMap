import argparse
import time
import brainflow
import numpy as np
import pyaudio
from brainflow.board_shim import BoardShim, BrainFlowInputParams, BoardIds, BrainFlowPresets
from brainflow.data_filter import DataFilter, FilterTypes, DetrendOperations
from pythonosc import udp_client

def main():

    BoardShim.enable_dev_board_logger()
    parser = argparse.ArgumentParser()
    # use docs to check which parameters are required for specific board, e.g. for Cyton - set serial port
    parser.add_argument('--timeout', type=int, help='timeout for device discovery or connection', required=False,
                        default=0)
    parser.add_argument('--ip-port', type=int, help='ip port', required=False, default=0)
    parser.add_argument('--ip-protocol', type=int, help='ip protocol, check IpProtocolType enum', required=False,
                        default=0)
    parser.add_argument('--ip-address', type=str, help='ip address', required=False, default='')
    parser.add_argument('--serial-port', type=str, help='serial port', required=False, default='/dev/cu.usbmodem11')
    parser.add_argument('--mac-address', type=str, help='mac address', required=False, default='')
    parser.add_argument('--other-info', type=str, help='other info', required=False, default='')
    parser.add_argument('--serial-number', type=str, help='serial number', required=False, default='')
    parser.add_argument('--board-id', type=int, help='board id, check docs to get a list of supported boards',
                        required=False, default=BoardIds.SYNTHETIC_BOARD)
    parser.add_argument('--file', type=str, help='file', required=False, default='')
    parser.add_argument('--master-board', type=int, help='master board id for streaming and playback boards',
                        required=False, default=BoardIds.NO_BOARD)
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

    sr = 44100

    board.start_stream()
    p = pyaudio.PyAudio()
    stream = p.open(rate= sr, channels=1, format= pyaudio.paInt16, output = True, output_device_index=1)
    CHUNK = 1024

    #data = board.get_current_board_data(1024)

    info = p.get_host_api_info_by_index(0)
    numdevices = info.get('deviceCount')

    for i in range(0, numdevices):
        if (p.get_device_info_by_host_api_device_index(0, i).get('maxOutputChannels')) > 0:
            print("Output Device id ", i, " - ", p.get_device_info_by_host_api_device_index(0, i).get('name'))

    try:
         while(True):
              
            data = board.get_current_board_data(CHUNK)

            sr = BoardShim.get_sampling_rate(args.board_id)
            #sr = 44100
            #print(sr)
            #channel_data = np.array([[]])
            for  ch in range(0, 7):      
                DataFilter.detrend(data[ch], DetrendOperations.CONSTANT.value)
                
                DataFilter.perform_bandpass(data[ch], sr, 3.0, 45.0, 2,
                                           FilterTypes.BUTTERWORTH_ZERO_PHASE, 0)
                DataFilter.perform_bandstop(data[ch], sr, 48.0, 52.0, 2,
                                           FilterTypes.BUTTERWORTH_ZERO_PHASE, 0)
                DataFilter.perform_bandstop(data[ch], sr, 58.0, 62.0, 2,
                                            FilterTypes.BUTTERWORTH_ZERO_PHASE, 0)
            
            #print(data.astype(np.int16))
             
            stream.write(data[0])
            #time.sleep(1024 / 44100) 

    except KeyboardInterrupt:
        print("stop and release")
        board.stop_stream()
        board.release_session()
        stream.stop_stream()
        stream.close()

        p.terminate()
        

if __name__ == "__main__":
        main()
