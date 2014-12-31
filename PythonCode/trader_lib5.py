import requests
import time
import json
import urllib
import threading
from optparse import OptionParser
import datetime



class ServerConnection(threading.Thread):
    """ServerConnection"""

    accountId = "5968094"
    token = '645ed9d76182140938834f0c240a9ac6-b088512f8d506024c55dd72d24423efb'
    url = "https://stream-fxpractice.oanda.com/v1/prices"
    order_url = "https://" + "api-fxpractice.oanda.com" + "/v1/accounts/" + accountId + "/orders"
    instrument = "EUR_USD"
    displayHeartbeat = False
    tick_event = threading.Event()
    time_out = 20006

    def __init__(self, test_mode):
        self.resp = self.connect_to_stream()
        if test_mode:
            self.url="http://127.0.0.1:8080"
            self.order_url="http://127.0.0.1:8080"
            # self.url="http://192.168.1.191:8080"
            # self.order_url="http://192.168.1.191:8080"

    def connect_to_stream(self):
        try:
            s = requests.Session()
            headers = {'Authorization' : 'Bearer ' + self.token,
                       # 'X-Accept-Datetime-Format' : 'unix'
                      }
            params = {'instruments' : self.instrument, 'accountId' : self.accountId}
            req = requests.Request('GET', self.url, headers = headers, params = params)
            pre = req.prepare()
            resp = s.send(pre, stream = True, verify = False, timeout=self.time_out)
            return resp
        except Exception as e:
            print "Caught exception when connecting to stream. Exception message:\n" + str(e) 
            s.close()

    def get_next(self,count):
        try:
            s = requests.Session()
            headers = {'Test':'get me next tick'}
            params = {'action' : 'getnext' + str(count)}
            req = requests.Request('GET', self.url, headers = headers, params = params)
            pre = req.prepare()
            resp = s.send(pre, stream = False, verify = False, timeout=self.time_out)
            return resp
        except Exception as e:
            print "Caught exception when connecting to stream. Exception message:\n" + str(e) 
            s.close()

    def start(self):
        response = self.connect_to_stream()
        if not response:
            return
        if response.status_code != 200:
            return
        try:
            for line in response.iter_lines(1):
                if line:
                    try:
                        msg = json.loads(line)
                    except Exception as e:
                        print "Caught exception when converting message into json. Exception message:\n" + str(e)
                        return
                    if self.displayHeartbeat:
                        print line
                        pass
                    else:
                        if msg.has_key("instrument") or msg.has_key("tick"):
                            print line
                            self.tick_event.set()
        except Exception as e:
            print "Caught exception when reading response. Exception message:\n" + str(e)



if __name__ == "__main__":
    sc = ServerConnection(test_mode = False)
    sc.start()
