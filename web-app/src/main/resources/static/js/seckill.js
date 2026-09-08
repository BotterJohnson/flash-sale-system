/**
 * 秒杀页面逻辑
 * 所有请求走网关 8081，token 由 common.js 的 $.ajaxSetup 统一注入。
 */

var seckillList = [];

// 活动状态：0 未开始 1 进行中 2 已结束
var STATUS_NOT_START = 0;
var STATUS_RUNNING = 1;
var STATUS_END = 2;

$(function () {
    g_initUserNav();
    g_refreshCartBadge();

    loadList();
    loadMine();
    // 每秒刷新倒计时
    setInterval(tick, 1000);
    // 每 5 秒同步一次库存与活动状态
    setInterval(loadList, 5000);
});

/* ---------------- 列表加载与渲染 ---------------- */

function loadList() {
    $.ajax({
        url: g_gatewayBase() + '/seckill/list',
        type: 'GET',
        xhrFields: { withCredentials: true },
        crossDomain: true,
        success: function (data) {
            if (data && data.code === 200) {
                seckillList = data.data || [];
                render();
            } else {
                toast((data && data.msg) || '加载秒杀列表失败');
            }
        },
        error: function () {
            // 轮询场景静默失败，避免刷屏
        }
    });
}

function render() {
    var grid = $('#seckillGrid');
    $('#listCount').text('共 ' + seckillList.length + ' 场活动');

    if (!seckillList.length) {
        grid.html(
            '<div class="empty">' +
            '<div class="icon">⏰</div>' +
            '<p>暂无秒杀活动</p>' +
            '<div class="sub">展开上方「发布 / 重置秒杀活动」，填入商品 ID 即可开启一场</div>' +
            '</div>'
        );
        return;
    }

    var html = '';
    for (var i = 0; i < seckillList.length; i++) {
        html += cardHtml(seckillList[i]);
    }
    grid.html(html);
    tick();
}

function cardHtml(item) {
    var sold = Math.max(item.total - item.stock, 0);
    var pct = item.total > 0 ? Math.round(sold * 100 / item.total) : 100;
    var soldOut = item.stock <= 0;

    var tagClass = item.status === STATUS_RUNNING ? 'tag-running'
        : (item.status === STATUS_NOT_START ? 'tag-wait' : 'tag-end');
    var tagText = item.status === STATUS_RUNNING ? '抢购中'
        : (item.status === STATUS_NOT_START ? '即将开始' : '已结束');

    var img = item.image
        ? '<img src="' + g_escapeHtml(item.image) + '" alt="' + g_escapeHtml(item.goodsName) + '">'
        : '<div class="s-img-ph">📦</div>';

    var btn;
    if (item.status === STATUS_END) {
        btn = '<button class="s-btn" disabled>活动已结束</button>';
    } else if (item.status === STATUS_NOT_START) {
        btn = '<button class="s-btn wait" disabled>即将开始</button>';
    } else if (item.bought) {
        btn = '<button class="s-btn done" onclick="showResult(\'' + item.orderId + '\')">已抢到 · 查看订单</button>';
    } else if (soldOut) {
        btn = '<button class="s-btn" disabled>已抢光</button>';
    } else {
        // goodsId 是 19 位雪花 ID，必须用引号包成字符串，否则 JS 当成 Number 丢精度（末尾变 400）
        btn = '<button class="s-btn" onclick="doSeckill(\'' + item.goodsId + '\')">立即抢购</button>';
    }

    return '<div class="s-card">' +
        '<div class="s-img">' + img +
            '<span class="s-status-tag ' + tagClass + '">' + tagText + '</span>' +
            '<span class="s-countdown" id="cd-' + item.goodsId + '">--:--:--</span>' +
        '</div>' +
        '<div class="s-body">' +
            '<div class="s-name">' + g_escapeHtml(item.goodsName) + '</div>' +
            '<div class="s-price">' +
                '<span class="now"><span class="cur">¥</span><span class="val">' + yuan(item.seckillPrice) + '</span></span>' +
                '<span class="old">原价 ¥' + yuan(item.originalPrice) + '</span>' +
            '</div>' +
            '<div class="s-progress-wrap">' +
                '<div class="s-progress"><div class="s-progress-bar' + (soldOut ? ' full' : '') + '" style="width:' + pct + '%"></div></div>' +
                '<div class="s-stock-text">' +
                    '<span>已抢 ' + sold + ' / ' + item.total + ' 件</span>' +
                    '<span>剩余 ' + item.stock + ' 件</span>' +
                '</div>' +
            '</div>' +
            btn +
        '</div>' +
    '</div>';
}

/* ---------------- 倒计时 ---------------- */

function tick() {
    var now = Date.now();
    for (var i = 0; i < seckillList.length; i++) {
        var item = seckillList[i];
        var el = document.getElementById('cd-' + item.goodsId);
        if (!el) {
            continue;
        }
        if (item.status === STATUS_END) {
            el.textContent = '活动已结束';
            continue;
        }
        var target = item.status === STATUS_NOT_START ? item.startTime : item.endTime;
        var prefix = item.status === STATUS_NOT_START ? '距开始 ' : '距结束 ';
        var left = target - now;
        if (left <= 0) {
            el.textContent = '00:00:00';
            loadList();
            continue;
        }
        el.textContent = prefix + formatLeft(left);
    }
}

function formatLeft(ms) {
    var s = Math.floor(ms / 1000);
    var h = Math.floor(s / 3600);
    var m = Math.floor((s % 3600) / 60);
    var sec = s % 60;
    return pad(h) + ':' + pad(m) + ':' + pad(sec);
}

function pad(n) {
    return n < 10 ? '0' + n : '' + n;
}

/* ---------------- 抢购 ---------------- */

function doSeckill(goodsId) {
    if (!g_getToken()) {
        toast('请先登录后再抢购');
        setTimeout(function () { location.href = '/login.html'; }, 900);
        return;
    }
    $.ajax({
        url: g_gatewayBase() + '/seckill/do?goodsId=' + goodsId,
        type: 'POST',
        xhrFields: { withCredentials: true },
        crossDomain: true,
        success: function (data) {
            if (data && data.code === 200) {
                showResult(data.data);
                loadMine();
            } else {
                toast((data && data.msg) || '抢购失败，请重试');
            }
            loadList();
        },
        error: function (xhr) {
            if (xhr && xhr.status === 401) {
                toast('登录已失效，请先登录');
                setTimeout(function () { location.href = '/login.html'; }, 900);
            } else if (xhr && xhr.status === 429) {
                toast('点太快啦，慢一点~');
            } else {
                toast('网络异常，请稍后重试');
            }
            loadList();
        }
    });
}

/* ---------------- 发布活动 ---------------- */

function publish() {
    var goodsId = $('#pGoodsId').val();
    var price = $('#pPrice').val();
    var stock = $('#pStock').val();
    if (!goodsId || !price || !stock) {
        toast('请填写商品 ID、秒杀价和库存');
        return;
    }
    $.ajax({
        url: g_gatewayBase() + '/seckill/publish',
        type: 'POST',
        data: {
            goodsId: goodsId,
            // 后端以「分」为单位存储
            seckillPrice: Math.round(parseFloat(price) * 100),
            stock: stock,
            delayMinutes: $('#pDelay').val() || 0,
            durationMinutes: $('#pDuration').val() || 30
        },
        xhrFields: { withCredentials: true },
        crossDomain: true,
        success: function (data) {
            if (data && data.code === 200) {
                toast('发布成功');
                loadList();
            } else {
                toast((data && data.msg) || '发布失败');
            }
        },
        error: function (xhr) {
            if (xhr && xhr.status === 401) {
                toast('请先登录后再发布活动');
            } else {
                toast('发布失败，请检查商品 ID 是否存在');
            }
        }
    });
}

/* ---------------- 重置活动 ---------------- */

/** 把库存刷回初始值、重新开始计时，方便反复压测 */
function resetActivity() {
    var goodsId = $('#pGoodsId').val();
    if (!goodsId) {
        toast('请先填写商品 ID');
        return;
    }
    $.ajax({
        url: g_gatewayBase() + '/seckill/reset',
        type: 'POST',
        data: {
            goodsId: goodsId,
            durationMinutes: $('#pDuration').val() || 30
        },
        xhrFields: { withCredentials: true },
        crossDomain: true,
        success: function (data) {
            if (data && data.code === 200) {
                toast('活动已重置');
                loadList();
            } else {
                toast((data && data.msg) || '重置失败');
            }
        },
        error: function (xhr) {
            toast(xhr && xhr.status === 401 ? '请先登录' : '重置失败，请检查商品 ID');
        }
    });
}

/* ---------------- 我的战绩 ---------------- */

function loadMine() {
    if (!g_getToken()) {
        renderMine(null);
        return;
    }
    $.ajax({
        url: g_gatewayBase() + '/seckill/myOrders',
        type: 'GET',
        xhrFields: { withCredentials: true },
        crossDomain: true,
        success: function (data) {
            renderMine(data && data.code === 200 ? data.data : null);
        },
        error: function () {
            // 静默失败，战绩面板不影响主流程
        }
    });
}

function renderMine(orders) {
    var box = $('#mineList');
    if (!orders) {
        box.html('<div class="mine-empty">登录后查看你的秒杀战绩</div>');
        return;
    }
    if (!orders.length) {
        box.html('<div class="mine-empty">还没有抢到任何商品，去试试手气吧~</div>');
        return;
    }
    var html = '';
    for (var i = 0; i < orders.length; i++) {
        var o = orders[i];
        html += '<div class="mine-item">' +
            '<div>' +
                '<div class="name">' + g_escapeHtml(o.goodsName) + '</div>' +
                '<div class="no">订单号 ' + g_escapeHtml(o.orderNo) + '</div>' +
            '</div>' +
            '<div class="price">¥' + yuan(o.payPrice) + '</div>' +
        '</div>';
    }
    box.html(html);
}

/* ---------------- 结果弹窗 & 提示 ---------------- */

function showResult(orderId) {
    $('#resultOrderId').text(orderId);
    $('#resultMask').addClass('show');
}

function closeResult() {
    $('#resultMask').removeClass('show');
}

$('#resultMask').on('click', function (e) {
    if (e.target === this) {
        closeResult();
    }
});

var toastTimer;
function toast(msg) {
    var t = document.getElementById('toast');
    t.textContent = msg;
    t.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { t.classList.remove('show'); }, 2200);
}

/** 分转元 */
function yuan(price) {
    return (price / 100).toFixed(2);
}
