#!/usr/bin/env python3
"""
seckill-service 并发压测脚本（零依赖，仅使用 Python 标准库）

用法示例：
    python seckill_bench.py --url http://localhost:8081 --goods-id 1 \
        --token <satoken> --concurrency 50 --requests 2000

参数说明：
    --url           网关地址
    --goods-id      秒杀活动对应的商品 ID
    --token         satoken（请求头），登录后从浏览器 localStorage / Cookie 拿
    --concurrency   并发线程数
    --requests      总请求数
    --timeout       单次请求超时秒数，默认 10

输出：
    总耗时 / QPS / 成功率 / 各状态码与业务码的分布 / 平均/最大/最小/P99 延迟
"""

import argparse
import json
import sys
import threading
import time
import urllib.parse
import urllib.request
from collections import Counter
from concurrent.futures import ThreadPoolExecutor, as_completed


def parse_args():
    p = argparse.ArgumentParser(description="seckill-service QPS benchmark")
    p.add_argument("--url", default="http://localhost:8081", help="网关地址")
    p.add_argument("--goods-id", type=int, required=True, help="秒杀活动对应的商品 ID")
    p.add_argument("--token", required=True, help="satoken 请求头")
    p.add_argument("--concurrency", type=int, default=50, help="并发线程数")
    p.add_argument("--requests", type=int, default=1000, help="总请求数")
    p.add_argument("--timeout", type=float, default=10.0, help="单次请求超时秒数")
    return p.parse_args()


def fire_one(url: str, goods_id: int, token: str, timeout: float):
    """单次抢购，返回 (http_status, business_code_or_-1, elapsed_ms)"""
    full = f"{url}/seckill/do?goodsId={goods_id}"
    req = urllib.request.Request(full, method="POST")
    req.add_header("satoken", token)
    req.add_header("Content-Type", "application/x-www-form-urlencoded")

    t0 = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            body = resp.read().decode("utf-8")
            http_status = resp.status
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", errors="replace")
        http_status = e.code
    except Exception:
        return http_status, -1, (time.perf_counter() - t0) * 1000
    elapsed_ms = (time.perf_counter() - t0) * 1000

    biz_code = -1
    try:
        biz_code = json.loads(body).get("code", -1)
    except Exception:
        pass
    return http_status, biz_code, elapsed_ms


def percentile(sorted_vals, p):
    if not sorted_vals:
        return 0.0
    k = max(0, min(len(sorted_vals) - 1, int(round(p / 100.0 * (len(sorted_vals) - 1)))))
    return sorted_vals[k]


def main():
    args = parse_args()
    print(f"[bench] url={args.url} goodsId={args.goods_id} token=***{args.token[-6:]}")
    print(f"[bench] concurrency={args.concurrency} total_requests={args.requests}")

    http_counter: Counter = Counter()
    biz_counter: Counter = Counter()
    latencies = []
    success = 0
    lock = threading.Lock()

    def task(_):
        http_status, biz_code, ms = fire_one(args.url, args.goods_id, args.token, args.timeout)
        with lock:
            http_counter[http_status] += 1
            biz_counter[biz_code] += 1
            latencies.append(ms)
            if http_status == 200 and biz_code == 200:
                success += 1

    t0 = time.perf_counter()
    with ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        list(as_completed(pool.submit(task, i) for i in range(args.requests)))
    total_ms = (time.perf_counter() - t0) * 1000

    latencies.sort()
    n = len(latencies)
    succ = sum(1 for _ in range(0))  # placeholder
    total = sum(http_counter.values())

    print()
    print("=" * 60)
    print(f"  总耗时        : {total_ms / 1000:8.2f} s")
    print(f"  实际 QPS      : {total / (total_ms / 1000):8.2f}")
    print(f"  成功率        : {100.0 * success / total:8.2f} %  ({success}/{total})")
    print("-" * 60)
    print("  HTTP 状态码分布 :")
    for k, v in sorted(http_counter.items(), key=lambda x: -x[1]):
        print(f"      {k} -> {v:>6}  ({100.0 * v / total:5.2f} %)")
    print("  业务码分布      :")
    for k, v in sorted(biz_counter.items(), key=lambda x: -x[1]):
        print(f"      {k} -> {v:>6}  ({100.0 * v / total:5.2f} %)")
    print("-" * 60)
    print(f"  平均延迟       : {sum(latencies) / n:8.2f} ms")
    print(f"  最小延迟       : {latencies[0]:8.2f} ms")
    print(f"  P50 延迟       : {percentile(latencies, 50):8.2f} ms")
    print(f"  P90 延迟       : {percentile(latencies, 90):8.2f} ms")
    print(f"  P99 延迟       : {percentile(latencies, 99):8.2f} ms")
    print(f"  最大延迟       : {latencies[-1]:8.2f} ms")
    print("=" * 60)

    # 业务码解读提示
    BIZ_MSG = {
        200: "秒杀成功",
        500800: "秒杀活动不存在",
        500801: "秒杀活动尚未开始",
        500802: "秒杀活动已结束",
        500803: "商品已被抢光",
        500804: "每人限购一件",
        500805: "扣库存失败",
        500102: "请先登录",
        -1: "网络/解析异常",
    }
    print("  业务码说明      :")
    seen = set()
    for code, _ in sorted(biz_counter.items(), key=lambda x: -x[1]):
        if code in seen:
            continue
        seen.add(code)
        print(f"      {code} = {BIZ_MSG.get(code, '未知')}")
    print()


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        sys.exit(1)
