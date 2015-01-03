f = open('test_data.txt','w')
line = "{\"tick\":{\"instrument\":\"EUR_TSD\",\"time\":\"2014-12-16T08:23:21.858010Z\",\"bid\":<bid>,\"ask\":<ask>}}\n"
ask = 1.24714
bid = 1.24701
for i in xrange(100):
	ask = ask + 0.00001
	bid = bid + 0.00001
	string = line.replace('<ask>',str(ask)).replace('<bid>',str(bid))
	f.write(string) # python will convert \n to os.linesep
f.close()