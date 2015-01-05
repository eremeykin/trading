import sqlite3
import json

con = sqlite3.connect('trades.db')
cur = con.cursor()

with open('data.txt') as f:
    content = f.readlines()

cur.execute("CREATE  TABLE \"main\".\"tradesUSD\" (\"dt\" VARCHAR, \"price\" FLOAT)")
for j_line in content:
	s_line = json.loads(j_line)
	# print("bid="+str(s_line['tick']['bid']))
	bid=s_line['tick']['bid']
	cur.execute("INSERT INTO \"main\".\"tradesUSD\" (\"dt\",\"price\") VALUES (null,"+str(bid)+")")
con.commit()
# cur.execute("SELECT * FROM trades")
# con.commit()
# next_set=cur.fetchmany(10)
# con.close()
# print(next_set)