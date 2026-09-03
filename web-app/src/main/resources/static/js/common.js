//展示loading
function g_showLoading(){
	var idx = layer.msg('处理中...', {icon: 16,shade: [0.5, '#f5f5f5'],scrollbar: false,offset: '0px', time:100000}) ;  
	return idx;
}
//salt
var g_passsword_salt="1a2b3c4d"
// 获取url参数
function g_getQueryString(name) {
	var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
	var r = window.location.search.substr(1).match(reg);
	if(r != null) return unescape(r[2]);
	return null;
};

//设定时间格式化函数，使用new Date().format("yyyyMMddhhmmss");
Date.prototype.format = function (format) {
    var args = {
        "M+": this.getMonth() + 1,
        "d+": this.getDate(),
        "h+": this.getHours(),
        "m+": this.getMinutes(),
        "s+": this.getSeconds(),
    };
    if (/(y+)/.test(format))
        format = format.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
    for (var i in args) {
        var n = args[i];
        if (new RegExp("(" + i + ")").test(format))
            format = format.replace(RegExp.$1, RegExp.$1.length == 1 ? n : ("00" + n).substr(("" + n).length));
    }
    return format;
};

// 全局 AJAX 拦截，把 localStorage 中的 token 自动塞入请求头
$.ajaxSetup({
    beforeSend: function(xhr, settings) {
        // 仅对网关(8081)请求注入 satoken 头，避免污染 search-service(18003) 等其它跨域服务
        var url = settings && settings.url;
        if (url && /:8081\//.test(url)) {
            var token = g_getToken();
            if (token) {
                xhr.setRequestHeader('satoken', token);
            }
        }
    }
});

// 网关地址：动态取当前页面 host，保证与页面同站，避免跨站导致 Cookie 无法保存
function g_gatewayBase() {
    return 'http://' + window.location.hostname + ':8081';
}

// 优先读 localStorage，其次从 Cookie 兜底(sa-token 服务器已写入 satoken)
function g_getToken() {
    var token = null;
    try {
        token = localStorage.getItem('token');
    } catch (e) {}
    if (token) {
        return token;
    }
    var m = document.cookie.match(/(?:^|;\s*)satoken=([^;]+)/);
    return m ? decodeURIComponent(m[1]) : null;
}

// 登录成功后额外写一份 Cookie，保证刷新/新开标签页也能保持登录态
function g_setTokenCookie(token) {
    if (!token) {
        return;
    }
    document.cookie = 'satoken=' + encodeURIComponent(token) + '; path=/; max-age=3600';
}

// 清除本地 token（localStorage + Cookie）
function g_clearToken() {
    try {
        localStorage.removeItem('token');
    } catch (e) {}
    document.cookie = 'satoken=; path=/; max-age=0';
}

// HTML 转义，防止昵称等用户数据注入
function g_escapeHtml(s) {
    if (s == null) {
        return '';
    }
    return String(s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

// 获取当前登录用户信息。回调: callback(err, user)
function g_getUserInfo(callback) {
    $.ajax({
        url: g_gatewayBase() + '/user/info',
        type: 'GET',
        xhrFields: { withCredentials: true },
        crossDomain: true,
        success: function (data) {
            if (data && data.code === 200) {
                callback(null, data.data);
            } else {
                callback((data && data.msg) || '未登录', null);
            }
        },
        error: function () {
            callback('网络异常，请稍后重试', null);
        }
    });
}

// 初始化右上角登录/用户信息（用于带 #userNav 的页面，如商品搜索）
function g_initUserNav() {
    var nav = document.getElementById('userNav');
    if (!nav) {
        return;
    }
    nav.innerHTML = '<a class="nav-link" href="/login.html">登录</a>';
    g_getUserInfo(function (err, user) {
        if (user) {
            var name = g_escapeHtml(user.nickname || user.mobile || '用户');
            nav.innerHTML =
                '<a class="nav-link" href="/user_info.html" title="个人中心">👤 ' + name + '</a>' +
                '<a class="nav-link" href="javascript:void(0)" onclick="g_logout()" title="退出登录">退出</a>';
        }
    });
}

// 刷新购物车角标：以后端 /order/cart/size 的真实数据为准。
// 页面上不存在 #cartBadge 时静默跳过，因此任何页面都可以安全调用。
// 未登录时直接置 0；网络异常时保留原值不清零，避免误报空车。
function g_refreshCartBadge() {
    var badge = document.getElementById('cartBadge');
    if (!badge) {
        return;
    }
    if (!g_getToken()) {
        badge.textContent = '0';
        return;
    }
    $.ajax({
        url: g_gatewayBase() + '/order/cart/size',
        type: 'GET',
        xhrFields: { withCredentials: true },
        crossDomain: true,
        success: function (data) {
            if (data && data.code === 200 && data.data) {
                // 默认展示总件数 total；如需展示种类数改为 data.data.kinds
                badge.textContent = data.data.total || 0;
            } else {
                badge.textContent = '0';
            }
        },
        error: function () {
            // 网络异常：保留当前显示，不做任何变更
        }
    });
}

// 退出登录：调用后端注销，完成后清除本地 token 并跳转登录页
function g_logout() {
    $.ajax({
        url: g_gatewayBase() + '/user/logout',
        type: 'POST',
        xhrFields: { withCredentials: true },
        crossDomain: true,
        complete: function () {
            g_clearToken();
            window.location.href = '/login.html';
        }
    });
}
