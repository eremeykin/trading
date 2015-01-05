import sqlite3
import matplotlib
from sklearn import cluster, datasets
import matplotlib.pyplot as plt
import statistics

con = sqlite3.connect('trades.db')
cur = con.cursor()
cur.execute("SELECT * FROM tradesUSD")
con.commit()

frame_len = 50  # длина рамки
n_f = 128 * 5  # количестово рамок
n_cl = 50  # количество кластеров
tail = 20  # длина хвоста
data = [-1 for i in range(0, n_f)]

for i in range(0, n_f):
    next_set = cur.fetchmany(frame_len)
    data[i] = [row[1] - next_set[0][1] for row in next_set]
con.close()

con = sqlite3.connect('trades.db')
cur = con.cursor()
cur.execute("SELECT * FROM tradesUSD")
con.commit()

data_t = [-1 for i in range(0, n_f)]
all=cur.fetchall()
for i in range(0, n_f):
    next_set = all[i*frame_len:(i+1)*frame_len+tail]
    data[i] = [row[1] - next_set[0][1] for row in next_set]
    next_set = all[i*frame_len:(i+1)*frame_len+tail]
    data_t[i] = [row[1] - next_set[0][1] for row in next_set]
con.close()

# data[i] - одна рамка длиной frame_len
# data - список рамок. Всего n_f рамок.
k_means = cluster.KMeans(n_clusters=n_cl)
k_means.fit(data)

cl = [[data[i] for i in range(0, n_f) if k_means.labels_[i] == j] for j in range(1, n_cl)]  # Clusters
clt = [[data_t[i] for i in range(0, n_f) if k_means.labels_[i] == j] for j in range(1, n_cl)]  # Clusters
# for j in range(0,n_cl-1):
# print(len(cl[j]))
# print(cl)
for j in range(1,min(20,len(cl[1]))):
    plt.plot(cl[1][j],)

for j in range(1,min(20,len(clt[1]))):
    plt.plot(clt[1][j])
plt.show()

# for cln in range(n_cl - 1):
#     clt = list(map(list, zip(*cl[cln])))  # Transposing
#     clm = list(map(statistics.mean, clt))  # Mean
#     plt.plot(clm)
#     plt.savefig("/home/eremeykin/PythonCode/clustering/clustering/img/mean{0:0>3}.png".format(cln + 1),
#                 bbox_inches='tight')
#     plt.close()
