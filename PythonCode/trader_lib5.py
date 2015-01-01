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
    time_out = 2000

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

    def order(self,instr, units, side, take_profit, stop_loss):
        try:
            s = requests.Session()
            s.keep_alive = False
            headers = {'Authorization' : 'Bearer ' + self.token,
                    'X-Accept-Datetime-Format' : 'unix',
                    'Connection':'close',
                    "Content-Type" : "application/x-www-form-urlencoded"
                    }

            params = urllib.urlencode({
                                    "instrument" : self.instrument,           
                                    "units" : units,                
                                    "type" : 'market',                # now!
                                    "side" : side,                     # "buy" price-up ("sell"  price-down)
                                    "takeProfit" : take_profit,    
                                    "stopLoss" : stop_loss
                                    })

            req =requests.post(self.order_url, data=params, headers=headers)
            for line in req.iter_lines(1):
                print "order responce: ", line
        except Exception as e:
             print "Caught exception when connecting to orders\n" + str(e) 

    def start(self):
        self.order(instr = self.instrument,units=10,side='buy',take_profit='10',stop_loss='20')
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
    sc = ServerConnection(test_mode = True)
    sc.order(instr = sc.instrument,units=10,side='buy',take_profit='10',stop_loss='20')
    # sc.start()
