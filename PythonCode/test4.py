from trader_lib4 import ServerConnection
import threading
import time

def  watch():
	print "Inside watch"
	for i in xrange(10):
		time.sleep(1)
		print "i=%d" %(i)
		# connection.tick_event.wait()
		if connection.tick_event.is_set():
			connection.tick_event.clear()
			print 'TICK'
			connection.get_next(i)
		else:
			pass
			# print 'waiting...'

connection = ServerConnection(True)
print "TEST"
thread = threading.Thread(target=watch)
thread.start()

connection.start()
# thread.join()

