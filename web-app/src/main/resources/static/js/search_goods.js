/* =====================================================
   search_goods.js  —  BotterShop
   Modern, animated, debounced search interaction
===================================================== */

var category     = null;
var brand        = null;
var specsValueMap = null;   // { specsName: value }
var page         = 1;
var orderField   = '';
var orderType    = '';
var minPrice     = null;
var maxPrice     = null;

var baseUrl      = 'http://127.0.0.1:18003/search';
var PAGE_SIZE    = 12;
var lastKeyword  = '';          // for tracking active search keyword

/* =====================================================
   Public search entry
   scrollTop: whether to scroll back to results on success
===================================================== */
function search(scrollTop) {
    var key = $('#key').val().trim();
    lastKeyword = key;
    try { sessionStorage.setItem('botter_last_key', key); } catch (e) {}

    // reset page when keyword or filters change (not when paginating)
    if (scrollTop !== false) page = 1;

    var hasFilter = !!(category || brand || specsValueMap || minPrice || maxPrice);
    if (!key && !hasFilter) {
        $('#activeFilters').hide();
        $('#toolbar').hide();
        $('#totalCount').text('输入关键词开始探索');
        $('#titleKeyword').hide();
        $('#pagination').empty();
        $('#goodsGrid').html(
            '<div class="empty"><div class="icon">🔍</div>'
          + '<p>在上方搜索框输入关键词，开始你的发现之旅</p></div>'
        );
        return;
    }

    var postParams = { key: key, orderField: orderField, orderType: orderType };
    if (brand != null)         postParams['brand']         = brand;
    if (category != null)      postParams['category']      = category;
    if (specsValueMap != null) postParams['specsValueMap'] = specsValueMap;
    if (minPrice != null)      postParams['minPrice']      = Math.round(minPrice * 100);
    if (maxPrice != null)      postParams['maxPrice']      = Math.round(maxPrice * 100);

    showSkeleton();

    $.ajax({
        url: baseUrl + '/goods/search/' + page + '/' + PAGE_SIZE,
        type: 'POST',
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify(postParams),
        success: function (data) {
            if (data && data.code == 200) {
                render(data.data);
                if (key) {
                    $('#titleKeyword').text('"' + key + '"').show();
                } else {
                    $('#titleKeyword').hide();
                }
            } else {
                showEmpty('⚠️', (data && data.msg) || '搜索失败，请稍后再试');
            }
        },
        error: function () {
            showEmpty('❌', '网络请求失败，请检查服务是否启动');
        }
    });
}

function searchByPage(p) { page = p; search(false); }

/* =====================================================
   Filter setters
===================================================== */
function searchByBrand(b)  { brand = b;  search(); }
function searchByCategory(c) { category = c; search(); }

function searchBySpecsValueMap(k, v) {
    if (specsValueMap == null) specsValueMap = {};
    if (specsValueMap[k] === v) {
        // toggle off
        delete specsValueMap[k];
        if (Object.keys(specsValueMap).length === 0) specsValueMap = null;
    } else {
        specsValueMap[k] = v;
    }
    search();
}

function searchByPrice() {
    var min = parseFloat($('#minPrice').val());
    var max = parseFloat($('#maxPrice').val());
    minPrice = isNaN(min) ? null : min;
    maxPrice = isNaN(max) ? null : max;
    if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
        showToast('⚠️ 最低价不能高于最高价');
        return;
    }
    page = 1;
    search();
    showToast('已应用价格筛选');
}

function resetPrice() {
    $('#minPrice').val(''); $('#maxPrice').val('');
    minPrice = null; maxPrice = null;
    page = 1; search();
}

/* =====================================================
   Order (sort buttons)
===================================================== */
function setOrder(btn, field, type) {
    orderField = field || '';
    orderType  = type  || '';
    $('.sort-btn').removeClass('active');
    $(btn).addClass('active');
    page = 1;
    search();
}

function clearFilters() {
    category = null; brand = null; specsValueMap = null;
    minPrice = null; maxPrice = null;
    orderField = ''; orderType = '';

    $('#minPrice').val(''); $('#maxPrice').val('');
    $('.sort-btn').removeClass('active');
    $('.sort-btn[data-order="default"]').addClass('active');

    $('#activeFilters').hide();
    page = 1;
    search();
    showToast('已清除全部筛选');
}

/* =====================================================
   Render
===================================================== */
function render(data) {
    data = data || {};

    /* ---------- Active filter chips ---------- */
    var tags = [];
    if (data.category) tags.push(makeClearChip('类目', data.category));
    if (data.brand)    tags.push(makeClearChip('品牌', data.brand));
    if (data.specsValueMap) {
        for (var k in data.specsValueMap) tags.push(makeClearChip(k, data.specsValueMap[k]));
    }
    var $tagsHost = $('#filterTags');
    $tagsHost.empty();
    if (tags.length > 0) {
        $tagsHost.append(tags.join(''));
        $('#activeFilters').show();
    } else {
        $('#activeFilters').hide();
    }

    /* ---------- Filter rows ---------- */
    renderCategory(data.categoryList, data.category);
    renderBrand(data.brandList, data.brand);
    renderSpecs(data.specsList, data.specsValueMap);

    /* ---------- Toolbar info ---------- */
    var totalPages = data.totalPages || 1;
    var cur = data.goodsEsInfoList ? data.goodsEsInfoList.length : 0;
    $('#totalCount').html('共 <b>' + (totalPages * PAGE_SIZE) + '+</b> 件商品');
    $('#toolbar').show();
    $('#toolbarInfo').html('为你找到 <b>' + (data.total || cur) + '</b> 件好物');

    /* ---------- Goods grid ---------- */
    var grid = $('#goodsGrid');
    grid.empty();
    if (!data.goodsEsInfoList || data.goodsEsInfoList.length === 0) {
        showEmpty('📭', '没有找到相关商品，试试其他关键词或筛选条件');
        $('#pagination').empty();
        return;
    }

    var frag = '';
    $.each(data.goodsEsInfoList, function (i, g) {
        frag += buildCard(g, i);
    });
    grid.append(frag);

    /* restore view mode */
    try {
        var v = localStorage.getItem('botter_view') || 'grid';
        if (v === 'list') grid.addClass('list');
    } catch (e) {}

    /* ---------- Pagination ---------- */
    renderPagination(totalPages);

    /* ---------- smooth scroll to results ---------- */
    if (page === 1) {
        var target = $('.toolbar').offset();
        if (target) $('html, body').animate({ scrollTop: target.top - 80 }, 320);
    }
}

/* ---------- chip builders ---------- */
function makeClearChip(label, val) {
    var id = 'chip_' + Math.random().toString(36).slice(2, 9);
    window['_clear_' + id] = function () {
        if (label === '类目') { category = null; }
        else if (label === '品牌') { brand = null; }
        else {
            if (specsValueMap) {
                delete specsValueMap[label];
                if (Object.keys(specsValueMap).length === 0) specsValueMap = null;
            }
        }
        search();
    };
    return '<span class="chip active" data-clear="' + id + '">'
         + escapeHtml(label) + ': ' + escapeHtml(val)
         + ' <span class="x" onclick="window[\'_clear_' + id + '\']()">×</span></span>';
}

/* ---------- renderers for each filter row ---------- */
function dedup(list) {
    if (!list) return [];
    var seen = {}, out = [];
    for (var i = 0; i < list.length; i++) {
        var v = list[i];
        if (v == null) continue;
        var k = String(v).trim();
        if (!k || seen[k]) continue;
        seen[k] = 1;
        out.push(v);
    }
    return out;
}

function renderCategory(list, current) {
    if (!list || list.length === 0 || current) { $('#categoryRow').hide(); return; }
    var html = dedup(list).map(function (c) {
        return '<span class="chip" onclick="searchByCategory(\'' + escapeAttr(c) + '\')">' + escapeHtml(c) + '</span>';
    }).join('');
    if (!html) { $('#categoryRow').hide(); return; }
    $('#categoryList').html(html);
    $('#categoryRow').show();
}

function renderBrand(list, current) {
    if (!list || list.length === 0 || current) { $('#brandRow').hide(); return; }
    var html = dedup(list).map(function (b) {
        return '<span class="chip" onclick="searchByBrand(\'' + escapeAttr(b) + '\')">' + escapeHtml(b) + '</span>';
    }).join('');
    if (!html) { $('#brandRow').hide(); return; }
    $('#brandList').html(html);
    $('#brandRow').show();
}

function renderSpecs(list, currentMap) {
    if (!list || Object.keys(list).length === 0) { $('#specsRow').hide(); return; }

    var html = '';
    var groups = [];
    for (var name in list) {
        // 每个分组内部去重
        var uniqOpts = dedup(list[name]);
        if (uniqOpts.length === 0) continue;
        groups.push({ name: name, opts: uniqOpts });
    }
    if (groups.length === 0) { $('#specsRow').hide(); return; }

    groups.forEach(function (g, gi) {
        if (gi > 0) html += '<div style="height:1px;background:var(--border);width:100%;margin:2px 0"></div>';
        html += '<div style="display:flex;flex-wrap:wrap;align-items:center;gap:8px;width:100%">';
        html += '<span class="specs-group-title">' + escapeHtml(g.name) + '：</span>';
        g.opts.forEach(function (opt) {
            var isActive = currentMap && currentMap[g.name] === opt;
            html += '<span class="chip' + (isActive ? ' active' : '') + '" '
                  + 'onclick="searchBySpecsValueMap(\'' + escapeAttr(g.name) + '\',\'' + escapeAttr(opt) + '\')">'
                  + escapeHtml(opt) + '</span>';
        });
        html += '</div>';
    });

    $('#specsList').html(html);
    $('#specsRow').show();
}

/* ---------- card builder ---------- */
function buildCard(g, i) {
    var price = g.price ? (g.price / 100) : 0;
    var stock = g.stock || 0;
    var outOfStock = stock <= 0;

    var stockBadge;
    if (outOfStock)           stockBadge = '<span class="g-stock-badge out">缺货</span>';
    else if (stock < 50)      stockBadge = '<span class="g-stock-badge low">仅剩 ' + stock + '</span>';
    else                      stockBadge = '<span class="g-stock-badge">现货 ' + stock + '</span>';

    var tag = (g.brandName || g.categoryName)
        ? '<span class="g-tag">' + renderHighlightedTitle(g.brandName || g.categoryName) + '</span>'
        : '';

    var imgHtml;
    if (g.image) {
        imgHtml = '<img src="' + escapeAttr(g.image) + '" alt="' + escapeAttr(g.name) + '" '
                + 'onerror="this.style.display=\'none\';this.nextSibling.style.display=\'flex\'">'
                + '<div class="g-img-placeholder" style="display:none">🛍</div>';
    } else {
        imgHtml = '<div class="g-img-placeholder">🛍</div>';
    }

    var metaParts = [];
    if (g.categoryName) metaParts.push('<span class="g-meta-item">' + renderHighlightedTitle(g.categoryName) + '</span>');
    if (g.brandName && g.brandName !== g.categoryName)
        metaParts.push('<span class="g-meta-item">' + renderHighlightedTitle(g.brandName) + '</span>');

    return ''
        + '<article class="g-card' + (outOfStock ? ' out-of-stock' : '') + '" '
        +   'style="animation-delay:' + (i * 35) + 'ms" data-id="' + escapeAttr(g.id || '') + '">'
        +   '<div class="g-img">' + tag + stockBadge + imgHtml + '</div>'
        +   '<div class="g-body">'
        +     '<h3 class="g-name" title="' + escapeAttr(stripHtml(g.name)) + '">'
        +       renderHighlightedTitle(g.name) + '</h3>'
        +     '<div class="g-meta">' + metaParts.join('') + '</div>'
        +     '<div class="g-footer">'
        +       '<div class="g-price"><span class="cur">¥</span><span class="val">' + price.toFixed(2) + '</span></div>'
        +       '<button class="g-cart" onclick="event.stopPropagation();addToCart(this)" '
        +         'title="' + (outOfStock ? '已售罄' : '加入购物车') + '" '
        +         (outOfStock ? 'disabled' : '') + '>'
        +         '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">'
        +           '<circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/>'
        +           '<path d="M1 1h4l2.7 13.4a2 2 0 0 0 2 1.6h9.7a2 2 0 0 0 2-1.6L23 6H6"/>'
        +         '</svg>'
        +       '</button>'
        +     '</div>'
        +   '</div>'
        + '</article>';
}

/* ---------- highlight title (safe) ----------
 * 后端 ES highlight 字段是 <span style="...">命中词</span>...
 * ⚠️ 关键：ES 会把 preTag/postTag 里的 HTML 字符实体化存到 highlight 字段，
 *   所以接口返回的字符串实际是：&lt;span style=&#39;...&#39;&gt;命中词&lt;/span&gt;...
 *   必须先反转义再用 DOM 解析。
 */
function renderHighlightedTitle(raw) {
    if (!raw) return '';
    var s = String(raw);

    // 1. 反转 HTML 实体
    if (s.indexOf('&') !== -1) {
        s = s
            .replace(/&lt;/gi, '<')
            .replace(/&gt;/gi, '>')
            .replace(/&quot;/gi, '"')
            .replace(/&#39;/g, "'")
            .replace(/&apos;/gi, "'")
            .replace(/&amp;/gi, '&');   // & 必须放最后
    }

    // 2. 如果没标签，原样 escape 返回
    if (s.indexOf('<') === -1) return escapeHtml(s);

    // 3. DOM 解析 + 白名单过滤
    var doc = document.implementation.createHTMLDocument('');
    doc.body.innerHTML = s;

    var whitelistTags = { SPAN: 1, B: 1, STRONG: 1, EM: 1, I: 1, U: 1, MARK: 1 };
    var allowedStyles = {
        'color': 1, 'font-weight': 1, 'text-decoration': 1,
        'background': 1, 'background-color': 1, 'font-style': 1
    };

    function walk(node) {
        var children = Array.prototype.slice.call(node.childNodes);
        for (var i = 0; i < children.length; i++) {
            var c = children[i];
            if (c.nodeType === 1) {
                var tag = c.nodeName;
                if (!whitelistTags[tag]) {
                    // 非白名单元素：拍平
                    var frag = doc.createDocumentFragment();
                    while (c.firstChild) frag.appendChild(c.firstChild);
                    c.parentNode.replaceChild(frag, c);
                } else {
                    // 白名单：清理属性/style
                    var attrs = Array.prototype.slice.call(c.attributes);
                    for (var j = 0; j < attrs.length; j++) {
                        var a = attrs[j];
                        if (a.name === 'style') {
                            var decls = a.value.split(';');
                            var kept = [];
                            for (var k = 0; k < decls.length; k++) {
                                var parts = decls[k].split(':');
                                if (parts.length !== 2) continue;
                                var prop = parts[0].trim().toLowerCase();
                                var val  = parts[1].trim();
                                if (!allowedStyles[prop]) continue;
                                if (/url\s*\(|expression\s*\(|javascript:/i.test(val)) continue;
                                kept.push(prop + ':' + val);
                            }
                            if (kept.length) c.setAttribute('style', kept.join(';'));
                            else c.removeAttribute('style');
                        } else {
                            c.removeAttribute(a.name);
                        }
                    }
                    c.classList.add('hl');
                    walk(c);
                }
            } else if (c.nodeType !== 3) {
                c.parentNode.removeChild(c);
            }
        }
    }
    walk(doc.body);
    return doc.body.innerHTML;
}

/* ---------- pagination ---------- */
function renderPagination(totalPages) {
    var host = $('#pagination');
    host.empty();
    if (totalPages <= 1) return;

    var html = '';
    html += '<button class="page-btn" ' + (page === 1 ? 'disabled' : '')
          + ' onclick="searchByPage(' + (page - 1) + ')">‹</button>';

    var start = Math.max(1, page - 2);
    var end   = Math.min(totalPages, start + 4);
    if (end - start < 4) start = Math.max(1, end - 4);

    if (start > 1) {
        html += '<button class="page-btn" onclick="searchByPage(1)">1</button>';
        if (start > 2) html += '<span style="color:var(--text-soft);padding:0 4px">…</span>';
    }
    for (var i = start; i <= end; i++) {
        html += '<button class="page-btn ' + (i === page ? 'active' : '') + '" onclick="searchByPage(' + i + ')">' + i + '</button>';
    }
    if (end < totalPages) {
        if (end < totalPages - 1) html += '<span style="color:var(--text-soft);padding:0 4px">…</span>';
        html += '<button class="page-btn" onclick="searchByPage(' + totalPages + ')">' + totalPages + '</button>';
    }

    html += '<button class="page-btn" ' + (page === totalPages ? 'disabled' : '')
          + ' onclick="searchByPage(' + (page + 1) + ')">›</button>';

    host.html(html);
}

/* =====================================================
   Loading & Empty
===================================================== */
function showSkeleton() {
    var html = '';
    for (var i = 0; i < 8; i++) {
        html += '<article class="g-card">'
              + '<div class="skeleton" style="width:100%;aspect-ratio:1/1"></div>'
              + '<div class="g-body">'
              + '<div class="skeleton" style="height:14px;margin-bottom:8px;border-radius:6px"></div>'
              + '<div class="skeleton" style="height:12px;width:60%;border-radius:6px;margin-bottom:14px"></div>'
              + '<div class="skeleton" style="height:24px;width:50%;border-radius:8px"></div>'
              + '</div></article>';
    }
    $('#goodsGrid').html(html);
    $('#toolbar').show();
    $('#pagination').empty();
}

function showEmpty(icon, msg) {
    $('#goodsGrid').html(
        '<div class="empty"><div class="icon">' + icon + '</div><p>' + escapeHtml(msg) + '</p></div>'
    );
    $('#pagination').empty();
    $('#toolbar').hide();
}

/* =====================================================
   Misc UI
===================================================== */
function addToCart(btn) {
    if (btn.disabled) return;
    var card = btn.closest('.g-card');
    var id   = card ? card.dataset.id : '';
    btn.style.transform = 'scale(1.2) rotate(0deg)';
    setTimeout(function(){ btn.style.transform = ''; }, 220);
    showToast('✓ 已加入愿望清单 (ID: ' + (id || '?') + ')');
    var badge = document.querySelector('.nav-link[title="购物车"] .badge');
    if (badge) badge.textContent = (parseInt(badge.textContent || '0', 10) + 1) + '';
}

/* =====================================================
   Utility
===================================================== */
function escapeHtml(s) {
    if (s == null) return '';
    return String(s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}
function escapeAttr(s) { return escapeHtml(s); }

/* 去除 HTML 标签，得到纯文本（用于 title 属性等） */
function stripHtml(s) {
    if (s == null) return '';
    var div = document.createElement('div');
    div.innerHTML = s;
    return div.textContent || div.innerText || '';
}

/* =====================================================
   Input debounce for price
===================================================== */
$(function () {
    /* ---------- hot keywords on first paint ---------- */
    var HOT = ['华为', 'iPhone', '小米', '电视', '笔记本', '耳机'];
    var hotHtml = HOT.map(function (k) {
        return '<span class="chip" onclick="$(\'#key\').val(\'' + k + '\');search()">' + k + '</span>';
    }).join('');
    var initialEmpty = '<div class="empty">'
        + '<div class="icon">🔍</div>'
        + '<p>在上方搜索框输入关键词，开始你的发现之旅</p>'
        + '<div class="suggestions">' + hotHtml + '</div>'
        + '</div>';
    $('#goodsGrid').html(initialEmpty);

    /* ---------- debounced input ---------- */
    var timer;
    $('#key').on('input', function () {
        clearTimeout(timer);
        timer = setTimeout(function () {
            if ($('#key').val().trim() !== lastKeyword) search();
        }, 450);
    });
});
