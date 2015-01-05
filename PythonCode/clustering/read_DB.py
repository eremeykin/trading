import sqlite3
import json

con = sqlite3.connect('trades.db')
cur = con.cursor()

with open('data.txt') as f:
    content = f.readlines()

for j_line in content:
	s_line = json.loads(j_line)
	print("bid="+str(s_line['tick']['bid'])
print ("exit")
# cur.execute("SELECT * FROM trades")
# con.commit()
# next_set=cur.fetchmany(10)
# con.close()
# print(next_set)