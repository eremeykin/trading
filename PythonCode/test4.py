from trader_lib5 import ServerConnection
import threading
import time

def  watch():
	print "Inside watch\n"
	while not connection.start:
		pass
	for i in xrange(1000):
		# print "inside loop"
		# print "i=%d" %(i)
		connection.get_next(id=0)
		# connection.tick_event.wait()
		# if connection.tick_event.is_set():
		# 	connection.tick_event.clear()
		# 	print 'TICK'
		# 	connection.get_next(i)
		# else:
		# 	pass
		# 	# print 'waiting...'
		# time.sleep(1)
	print "R E A D Y"

connection = ServerConnection(True)
print "start"
thread = threading.Thread(target=watch)
thread.start()
connection.start()

# thread.join()

