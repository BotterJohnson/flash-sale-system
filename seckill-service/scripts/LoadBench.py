"""
诊断用对照压测：绕过网关直压 seckill-service vs 走网关，定位瓶颈层。

用法：
    python LoadBench.py <host> <port> [总请求数] [并发数]

说明：
    - 从 scripts/data/tokens.csv 读 token（这些用户已抢过，走快速失败路径，
      请求只做 token 校验 + 已购判断，不查库、不发 MQ，反映纯链路吞吐）
    - 每个线程持有一条 keep-alive 连接，模拟长连接压测
"""
import csv
import http.client
import statistics
import sys
import threading
import time
from pathlib import Path

HOST = sys.argv[1] if len(sys.argv) > 1 else "localhost"
PORT = int(sys.argv[2]) if len(sys.argv) > 2 else 18007
TOTAL = int(sys.argv[3]) if len(sys.argv) > 3 else 2000
CONC = int(sys.argv[4]) if len(sys.argv) > 4 else 200

GOODS_ID = "1432247758961553408"
TOKENS_CSV = (Path(__file__).resolve().parent.parent
              / "src/main/java/com/botter/shop/seckill/scripts/data/tokens.csv")

tokens = []
with open(TOKENS_CSV, newline="", encoding="utf-8") as f:
    for row in csv.reader(f):
        if len(row) >= 2 and row[1].strip():
            tokens.append(row[1].strip())
tokens = tokens[:CONC]
print(f"target={HOST}:{PORT}  total={TOTAL}  concurrency={CONC}  tokens={len(tokens)}")

latencies = []
errors = []
lock = threading.Lock()
counter = {"i": 0}


def worker(tid: int):
    conn = http.client.HTTPConnection(HOST, PORT, timeout=30)
    while True:
        with lock:
            i = counter["i"]
            if i >= TOTAL:
                return
            counter["i"] = i + 1
        token = tokens[i % len(tokens)]
        headers = {"satoken": token}
        t0 = time.perf_counter()
        try:
            conn.request("POST", f"/seckill/do?goodsId={GOODS_ID}", headers=headers)
            resp = conn.getresponse()
            body = resp.read()
            cost = (time.perf_counter() - t0) * 1000
            with lock:
                latencies.append(cost)
                if resp.status != 200:
                    errors.append(f"HTTP {resp.status}")
        except Exception as e:  # 连接断了就重建
            with lock:
                errors.append(repr(e))
            try:
                conn.close()
            except Exception:
                pass
            conn = http.client.HTTPConnection(HOST, PORT, timeout=30)


ts = [threading.Thread(target=worker, args=(i,)) for i in range(CONC)]
t_start = time.perf_counter()
for t in ts:
    t.start()
for t in ts:
    t.join()
elapsed = time.perf_counter() - t_start

lat = sorted(latencies)
n = len(lat)


def pct(p):
    return lat[min(int(n * p), n - 1)] if n else 0


print(f"\n完成 {n} 个请求, 失败 {len(errors)} 个, 耗时 {elapsed:.2f}s")
print(f"吞吐: {n / elapsed:.1f} req/s")
if n:
    print(f"延迟ms: min={lat[0]:.0f} p50={pct(0.5):.0f} p90={pct(0.9):.0f} "
          f"p99={pct(0.99):.0f} max={lat[-1]:.0f} avg={statistics.mean(lat):.0f}")
if errors:
    from collections import Counter
    print("错误分布:", Counter(errors).most_common(5))
