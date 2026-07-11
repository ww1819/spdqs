(function () {
    'use strict';

    const wrapEl = document.getElementById('flowCanvasWrap');
    const projectId = wrapEl?.dataset.projectId;
    const canvas = document.getElementById('flowCanvas');
    const linesSvg = document.getElementById('flowLines');
    const loadingEl = document.getElementById('flowLoading');
    const toastEl = document.getElementById('flowToast');
    const tableWrap = document.getElementById('flowTableWrap');
    const mergeHead = document.getElementById('flowMergeHead');
    const mergeBody = document.getElementById('flowMergeBody');
    const btnViewTree = document.getElementById('btnViewTree');
    const btnViewTable = document.getElementById('btnViewTable');
    const menuPopup = document.getElementById('menuActionPopup');

    const newFlowModal = new bootstrap.Modal(document.getElementById('newFlowModal'));
    const editFlowModal = new bootstrap.Modal(document.getElementById('editFlowModal'));
    const changeLogModal = new bootstrap.Modal(document.getElementById('changeLogModal'));
    const copyFallbackModal = new bootstrap.Modal(document.getElementById('copyFallbackModal'));

    let treeData = null;
    let pendingParentId = null;
    let activeNodeId = null;
    let popupNode = null;
    let editingNode = null;
    let currentView = localStorage.getItem('spdqs-analysis-view') || 'tree';

    function csrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        const headers = { 'Content-Type': 'application/json' };
        if (token && header) {
            headers[header] = token;
        }
        return headers;
    }

    function showToast(msg) {
        if (!toastEl) return;
        toastEl.textContent = msg;
        toastEl.classList.add('show');
        setTimeout(() => toastEl.classList.remove('show'), 2500);
    }

    function ticketListUrl(menuTitle) {
        return '/tickets?menu=' + encodeURIComponent(menuTitle);
    }

    async function readJsonResponse(res) {
        const contentType = res.headers.get('content-type') || '';
        const text = await res.text();
        if (!contentType.includes('application/json')) {
            if (text.includes('<!DOCTYPE') || text.includes('<html')) {
                throw new Error('接口返回异常，请重新登录或重启应用后再试');
            }
            throw new Error(text || '服务器返回格式错误');
        }
        try {
            return JSON.parse(text);
        } catch (e) {
            throw new Error('解析数据失败');
        }
    }

    async function loadTree() {
        if (!projectId) {
            throw new Error('项目 ID 无效');
        }
        const res = await fetch('/api/analysis/' + encodeURIComponent(projectId) + '/tree', {
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' }
        });
        const data = await readJsonResponse(res);
        if (!res.ok) {
            throw new Error(data.error || '加载失败（' + res.status + '）');
        }
        treeData = data;
        renderCurrentView();
    }

    function updateViewButtons() {
        if (btnViewTree && btnViewTable) {
            btnViewTree.classList.toggle('btn-primary', currentView === 'tree');
            btnViewTree.classList.toggle('btn-outline-primary', currentView !== 'tree');
            btnViewTable.classList.toggle('btn-primary', currentView === 'table');
            btnViewTable.classList.toggle('btn-outline-primary', currentView !== 'table');
        }
    }

    function setView(view) {
        currentView = view === 'table' ? 'table' : 'tree';
        localStorage.setItem('spdqs-analysis-view', currentView);
        updateViewButtons();
        hideMenuPopup();
        if (wrapEl) {
            wrapEl.classList.toggle('d-none', currentView !== 'tree');
        }
        if (tableWrap) {
            tableWrap.classList.toggle('d-none', currentView !== 'table');
        }
        renderCurrentView();
    }

    function renderCurrentView() {
        if (!treeData) return;
        if (currentView === 'table') {
            renderMergeTable();
        } else {
            renderTree();
        }
    }

    function renderTree() {
        if (!treeData || !canvas) return;
        canvas.innerHTML = '';
        const root = document.createElement('div');
        root.className = 'flow-tree-root';
        root.appendChild(renderNode(treeData));
        canvas.appendChild(root);
        requestAnimationFrame(() => requestAnimationFrame(drawLines));
    }

    function findNodeById(node, id) {
        if (!node) return null;
        if (node.id === id) return node;
        if (!node.children) return null;
        for (const child of node.children) {
            const found = findNodeById(child, id);
            if (found) return found;
        }
        return null;
    }

    function createMenuTrigger(node) {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'flow-menu-trigger';
        btn.textContent = node.title;
        btn.title = '点击打开操作菜单';
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            showMenuPopup(node, e.currentTarget);
        });
        return btn;
    }

    function showMenuPopup(node, anchorEl) {
        popupNode = node;
        if (!menuPopup || !anchorEl) return;
        menuPopup.classList.remove('d-none');
        const rect = anchorEl.getBoundingClientRect();
        const popupWidth = 168;
        let left = rect.left;
        let top = rect.bottom + 6;
        if (left + popupWidth > window.innerWidth - 8) {
            left = window.innerWidth - popupWidth - 8;
        }
        if (top + 180 > window.innerHeight) {
            top = Math.max(8, rect.top - 180);
        }
        menuPopup.style.left = left + 'px';
        menuPopup.style.top = top + 'px';
    }

    function hideMenuPopup() {
        popupNode = null;
        if (menuPopup) {
            menuPopup.classList.add('d-none');
        }
    }

    function renderNode(node) {
        const wrap = document.createElement('div');
        wrap.className = 'flow-node-wrap';
        wrap.dataset.nodeId = node.id;

        const zone = document.createElement('div');
        zone.className = 'flow-card-zone';

        const card = document.createElement('div');
        card.className = 'flow-card';

        const toolbar = document.createElement('div');
        toolbar.className = 'flow-card-toolbar';
        toolbar.innerHTML =
            '<button type="button" class="flow-tool-btn" data-action="new">新建流程</button>' +
            '<button type="button" class="flow-tool-btn" data-action="edit">修改</button>' +
            '<button type="button" class="flow-tool-btn" data-action="history">变更记录</button>' +
            '<button type="button" class="flow-tool-btn flow-tool-danger" data-action="delete">删除</button>';

        const body = document.createElement('div');
        body.className = 'flow-card-body';
        const title = document.createElement('button');
        title.type = 'button';
        title.className = 'flow-card-title flow-card-title-link';
        title.textContent = node.title;
        title.title = '点击打开操作菜单';
        title.addEventListener('click', (e) => {
            e.stopPropagation();
            showMenuPopup(node, e.currentTarget);
        });
        body.appendChild(title);
        if (node.hasDescription) {
            const dot = document.createElement('span');
            dot.className = 'flow-desc-dot';
            dot.title = '已填写功能说明';
            body.appendChild(dot);
        }

        card.appendChild(body);
        zone.appendChild(toolbar);
        zone.appendChild(card);
        wrap.appendChild(zone);

        toolbar.querySelector('[data-action="new"]').addEventListener('click', (e) => {
            e.stopPropagation();
            openNewFlow(node.id);
        });
        toolbar.querySelector('[data-action="edit"]').addEventListener('click', (e) => {
            e.stopPropagation();
            openEdit(node);
        });
        toolbar.querySelector('[data-action="history"]').addEventListener('click', (e) => {
            e.stopPropagation();
            openChangeLog(node);
        });
        toolbar.querySelector('[data-action="delete"]').addEventListener('click', (e) => {
            e.stopPropagation();
            deleteNode(node);
        });

        if (node.children && node.children.length > 0) {
            const childrenRow = document.createElement('div');
            childrenRow.className = 'flow-children';

            node.children.forEach((child) => {
                const branch = document.createElement('div');
                branch.className = 'flow-branch';
                branch.appendChild(renderNode(child));
                childrenRow.appendChild(branch);
            });

            wrap.appendChild(childrenRow);
        }

        return wrap;
    }

    function flattenPaths(node, path) {
        const current = path.concat([{
            id: node.id,
            title: node.title,
            hasDescription: !!node.hasDescription
        }]);
        if (!node.children || node.children.length === 0) {
            return [current];
        }
        const rows = [];
        node.children.forEach((child) => {
            flattenPaths(child, current).forEach((row) => rows.push(row));
        });
        return rows;
    }

    function computeRowspans(rows, depth) {
        const spans = rows.map(() => new Array(depth).fill(0));
        for (let col = 0; col < depth; col++) {
            let i = 0;
            while (i < rows.length) {
                let j = i + 1;
                while (j < rows.length) {
                    let same = true;
                    for (let c = 0; c <= col; c++) {
                        const a = rows[i][c];
                        const b = rows[j][c];
                        if (!a || !b || a.id !== b.id) {
                            same = false;
                            break;
                        }
                    }
                    if (!same) break;
                    j++;
                }
                spans[i][col] = j - i;
                for (let k = i + 1; k < j; k++) {
                    spans[k][col] = 0;
                }
                i = j;
            }
        }
        return spans;
    }

    function appendCellContent(container, cell, float) {
        const trigger = createMenuTrigger(cell);
        if (float) {
            const sticky = document.createElement('div');
            sticky.className = 'flow-sticky-label';
            sticky.appendChild(trigger);
            if (cell.hasDescription) {
                const dot = document.createElement('span');
                dot.className = 'flow-desc-dot';
                dot.title = '已填写功能说明';
                sticky.appendChild(dot);
            }
            container.appendChild(sticky);
            return;
        }
        container.appendChild(trigger);
        if (cell.hasDescription) {
            const dot = document.createElement('span');
            dot.className = 'flow-desc-dot';
            dot.title = '已填写功能说明';
            container.appendChild(dot);
        }
    }

    /** 合并单元格内名称随滚动浮动到可视区（CSS sticky 对 rowspan 不可靠） */
    function updateFloatingLabels() {
        const scrollEl = document.getElementById('flowTableScroll');
        if (!scrollEl) return;
        const scrollRect = scrollEl.getBoundingClientRect();
        const headerEl = scrollEl.querySelector('thead th');
        const headerH = headerEl ? headerEl.getBoundingClientRect().height : 40;
        const visibleTop = scrollRect.top + headerH + 6;

        scrollEl.querySelectorAll('.flow-merge-cell .flow-sticky-label').forEach((label) => {
            const cell = label.closest('td');
            if (!cell) return;
            const cellRect = cell.getBoundingClientRect();
            const labelH = label.offsetHeight || 28;
            const maxOffset = Math.max(0, cell.clientHeight - labelH - 12);
            let offset = visibleTop - cellRect.top;
            if (offset < 0) offset = 0;
            if (offset > maxOffset) offset = maxOffset;
            label.style.transform = 'translateY(' + offset + 'px)';
        });
    }

    function bindFloatingLabelScroll() {
        const scrollEl = document.getElementById('flowTableScroll');
        if (!scrollEl || scrollEl.dataset.floatBound === '1') return;
        scrollEl.dataset.floatBound = '1';
        scrollEl.addEventListener('scroll', () => {
            window.requestAnimationFrame(updateFloatingLabels);
        }, { passive: true });
        window.addEventListener('resize', () => {
            window.requestAnimationFrame(updateFloatingLabels);
        });
    }

    function renderMergeTable() {
        if (!treeData || !mergeHead || !mergeBody) return;
        const rows = flattenPaths(treeData, []);
        const depth = rows.reduce((max, row) => Math.max(max, row.length), 0);
        if (depth === 0) {
            mergeHead.innerHTML = '';
            mergeBody.innerHTML = '<tr><td class="text-muted">暂无流程节点</td></tr>';
            return;
        }

        const pinCount = Math.min(2, depth);
        const headRow = document.createElement('tr');
        for (let i = 0; i < depth; i++) {
            const th = document.createElement('th');
            th.textContent = '第' + (i + 1) + '级';
            if (i < pinCount) {
                th.classList.add('flow-col-pin-' + i);
            }
            headRow.appendChild(th);
        }
        const actionTh = document.createElement('th');
        actionTh.className = 'flow-merge-sticky';
        actionTh.textContent = '操作';
        headRow.appendChild(actionTh);
        mergeHead.innerHTML = '';
        mergeHead.appendChild(headRow);

        const spans = computeRowspans(rows, depth);
        mergeBody.innerHTML = '';

        rows.forEach((row, rowIndex) => {
            const tr = document.createElement('tr');
            for (let col = 0; col < depth; col++) {
                const span = spans[rowIndex][col];
                if (span === 0) continue;
                const cell = row[col];
                const td = document.createElement('td');
                td.rowSpan = span;
                td.className = 'flow-merge-cell';
                if (col < pinCount) {
                    td.classList.add('flow-col-pin-' + col);
                }
                if (cell) {
                    const isAncestor = col < row.length - 1;
                    if (isAncestor) {
                        td.classList.add('is-ancestor');
                    }
                    // 前两级及所有父级/大合并格都启用浮动标签
                    const needFloat = isAncestor || span > 1 || col < pinCount;
                    appendCellContent(td, cell, needFloat);
                } else {
                    td.textContent = '—';
                }
                tr.appendChild(td);
            }

            const leaf = row[row.length - 1];
            const actionTd = document.createElement('td');
            actionTd.className = 'cell-actions flow-merge-actions flow-merge-sticky';
            if (leaf) {
                const btnEdit = document.createElement('button');
                btnEdit.type = 'button';
                btnEdit.className = 'btn btn-link btn-sm p-0';
                btnEdit.textContent = '修改';
                btnEdit.addEventListener('click', () => {
                    const node = findNodeById(treeData, leaf.id);
                    if (node) openEdit(node);
                });

                const btnHistory = document.createElement('button');
                btnHistory.type = 'button';
                btnHistory.className = 'btn btn-link btn-sm p-0';
                btnHistory.textContent = '变更记录';
                btnHistory.addEventListener('click', () => {
                    const node = findNodeById(treeData, leaf.id);
                    if (node) openChangeLog(node);
                });

                actionTd.appendChild(btnEdit);
                actionTd.appendChild(document.createTextNode(' '));
                actionTd.appendChild(btnHistory);
            }
            tr.appendChild(actionTd);
            mergeBody.appendChild(tr);
        });

        bindFloatingLabelScroll();
        requestAnimationFrame(() => requestAnimationFrame(updateFloatingLabels));
    }

    function getCardElement(nodeWrap) {
        return nodeWrap.querySelector(':scope > .flow-card-zone > .flow-card');
    }

    function addLine(linesSvgEl, x1, y1, x2, y2) {
        const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
        line.setAttribute('x1', x1);
        line.setAttribute('y1', y1);
        line.setAttribute('x2', x2);
        line.setAttribute('y2', y2);
        line.setAttribute('class', 'flow-line');
        linesSvgEl.appendChild(line);
    }

    function drawLines() {
        if (!linesSvg || !canvas || currentView !== 'tree') return;
        const wrap = canvas.querySelector('.flow-tree-root');
        if (!wrap) return;

        const container = linesSvg.parentElement;
        const containerRect = container.getBoundingClientRect();
        linesSvg.setAttribute('width', container.scrollWidth);
        linesSvg.setAttribute('height', container.scrollHeight);
        linesSvg.innerHTML = '';

        canvas.querySelectorAll('.flow-node-wrap').forEach((nodeWrap) => {
            const childrenRow = nodeWrap.querySelector(':scope > .flow-children');
            if (!childrenRow) return;

            const parentCard = getCardElement(nodeWrap);
            if (!parentCard) return;

            const parentRect = parentCard.getBoundingClientRect();
            const px = parentRect.left + parentRect.width / 2 - containerRect.left + container.scrollLeft;
            const py = parentRect.bottom - containerRect.top + container.scrollTop;

            const branches = childrenRow.querySelectorAll(':scope > .flow-branch');
            const childPoints = [];
            branches.forEach((branch) => {
                const childWrap = branch.querySelector(':scope > .flow-node-wrap');
                const childCard = childWrap ? getCardElement(childWrap) : branch.querySelector('.flow-card');
                if (!childCard) return;
                const childRect = childCard.getBoundingClientRect();
                childPoints.push({
                    x: childRect.left + childRect.width / 2 - containerRect.left + container.scrollLeft,
                    y: childRect.top - containerRect.top + container.scrollTop
                });
            });

            if (childPoints.length === 0) return;

            const minChildY = Math.min(...childPoints.map((p) => p.y));
            const midY = py + (minChildY - py) / 2;

            if (childPoints.length === 1) {
                const child = childPoints[0];
                addLine(linesSvg, px, py, px, midY);
                addLine(linesSvg, px, midY, child.x, midY);
                addLine(linesSvg, child.x, midY, child.x, child.y);
                return;
            }

            const minX = Math.min(...childPoints.map((p) => p.x));
            const maxX = Math.max(...childPoints.map((p) => p.x));

            addLine(linesSvg, px, py, px, midY);
            addLine(linesSvg, minX, midY, maxX, midY);
            childPoints.forEach((child) => {
                addLine(linesSvg, child.x, midY, child.x, child.y);
            });
        });
    }

    function openNewFlow(parentId) {
        pendingParentId = parentId;
        document.getElementById('newFlowTitle').value = '';
        newFlowModal.show();
        setTimeout(() => document.getElementById('newFlowTitle').focus(), 200);
    }

    function setTitleEditMode(editing) {
        document.getElementById('editTitleView').classList.toggle('d-none', editing);
        document.getElementById('editFlowTitle').classList.toggle('d-none', !editing);
        document.getElementById('btnEditTitle').classList.toggle('d-none', editing);
        document.getElementById('btnSaveTitle').classList.toggle('d-none', !editing);
        document.getElementById('btnCancelTitle').classList.toggle('d-none', !editing);
    }

    function setDescEditMode(editing) {
        document.getElementById('editDescView').classList.toggle('d-none', editing);
        document.getElementById('editFlowDesc').classList.toggle('d-none', !editing);
        document.getElementById('btnEditDesc').classList.toggle('d-none', editing);
        document.getElementById('btnSaveDesc').classList.toggle('d-none', !editing);
        document.getElementById('btnCancelDesc').classList.toggle('d-none', !editing);
    }

    function refreshEditViews() {
        const node = editingNode || {};
        document.getElementById('editTitleView').textContent = node.title || '—';
        document.getElementById('editFlowTitle').value = node.title || '';
        const desc = node.description && String(node.description).trim()
            ? node.description
            : '（暂无功能描述）';
        document.getElementById('editDescView').textContent = desc;
        document.getElementById('editFlowDesc').value = node.description || '';
        setTitleEditMode(false);
        setDescEditMode(false);
    }

    function openEdit(node) {
        activeNodeId = node.id;
        editingNode = node;
        refreshEditViews();
        editFlowModal.show();
    }

    async function openChangeLog(node) {
        document.getElementById('changeLogModalTitle').textContent = node.title + ' — 变更记录';
        const emptyEl = document.getElementById('changeLogEmpty');
        const listEl = document.getElementById('changeLogList');
        emptyEl.classList.add('d-none');
        listEl.innerHTML = '<div class="text-muted">加载中…</div>';
        changeLogModal.show();

        try {
            const res = await fetch('/api/analysis/nodes/' + encodeURIComponent(node.id) + '/changes', {
                credentials: 'same-origin',
                headers: { 'Accept': 'application/json' }
            });
            const data = await readJsonResponse(res);
            if (!res.ok) {
                throw new Error(data.error || '加载失败');
            }
            if (!data.length) {
                listEl.innerHTML = '';
                emptyEl.classList.remove('d-none');
                return;
            }
            emptyEl.classList.add('d-none');
            listEl.innerHTML = '';
            data.forEach((item) => {
                const card = document.createElement('div');
                card.className = 'change-log-item';

                const meta = document.createElement('div');
                meta.className = 'change-log-meta';
                meta.innerHTML =
                    '<span>修改人：' + escapeHtml(item.changeBy || '—') + '</span>' +
                    '<span>时间：' + escapeHtml(formatDateTime(item.changeTime)) + '</span>';
                card.appendChild(meta);

                if (item.titleChanged) {
                    const row = document.createElement('div');
                    row.className = 'change-log-row';
                    row.innerHTML = '<strong>曾用名</strong>' +
                        escapeHtml(item.oldTitle || '—') +
                        ' → ' +
                        escapeHtml(item.newTitle || '—');
                    card.appendChild(row);
                }
                if (item.descriptionChanged) {
                    const oldRow = document.createElement('div');
                    oldRow.className = 'change-log-row';
                    oldRow.innerHTML = '<strong>原功能描述</strong>' + escapeHtml(item.oldDescription || '（空）');
                    card.appendChild(oldRow);
                    const newRow = document.createElement('div');
                    newRow.className = 'change-log-row';
                    newRow.innerHTML = '<strong>新功能描述</strong>' + escapeHtml(item.newDescription || '（空）');
                    card.appendChild(newRow);
                }
                listEl.appendChild(card);
            });
        } catch (e) {
            listEl.innerHTML = '<div class="text-danger">' + escapeHtml(e.message || '加载失败') + '</div>';
        }
    }

    function escapeHtml(text) {
        return String(text == null ? '' : text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function formatDateTime(value) {
        if (!value) return '—';
        const d = new Date(value);
        if (Number.isNaN(d.getTime())) return String(value);
        const pad = (n) => String(n).padStart(2, '0');
        return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
            + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
    }

    document.getElementById('btnConfirmNewFlow').addEventListener('click', async () => {
        const title = document.getElementById('newFlowTitle').value.trim();
        if (!title) {
            alert('请输入流程标题');
            return;
        }
        try {
            const res = await fetch('/api/analysis/nodes', {
                method: 'POST',
                credentials: 'same-origin',
                headers: csrfHeaders(),
                body: JSON.stringify({ parentId: pendingParentId, title: title })
            });
            const data = await readJsonResponse(res);
            if (!res.ok) {
                throw new Error(data.error || '创建失败');
            }
            treeData = data;
            newFlowModal.hide();
            renderCurrentView();
            showToast('流程已创建');
        } catch (e) {
            alert(e.message || '创建失败');
        }
    });

    document.getElementById('btnEditTitle').addEventListener('click', () => {
        document.getElementById('editFlowTitle').value = editingNode?.title || '';
        setTitleEditMode(true);
        document.getElementById('editFlowTitle').focus();
    });
    document.getElementById('btnCancelTitle').addEventListener('click', () => {
        document.getElementById('editFlowTitle').value = editingNode?.title || '';
        setTitleEditMode(false);
    });
    document.getElementById('btnSaveTitle').addEventListener('click', async () => {
        const title = document.getElementById('editFlowTitle').value.trim();
        if (!title) {
            alert('请输入菜单名称');
            return;
        }
        try {
            const res = await fetch('/api/analysis/nodes/' + activeNodeId, {
                method: 'PUT',
                credentials: 'same-origin',
                headers: csrfHeaders(),
                body: JSON.stringify({ title: title })
            });
            const data = await readJsonResponse(res);
            if (!res.ok) {
                throw new Error(data.error || '保存失败');
            }
            treeData = data;
            editingNode = findNodeById(treeData, activeNodeId) || editingNode;
            if (editingNode) {
                editingNode.title = title;
            }
            refreshEditViews();
            renderCurrentView();
            showToast('名称已更新');
        } catch (e) {
            alert(e.message || '保存失败');
        }
    });

    document.getElementById('btnEditDesc').addEventListener('click', () => {
        document.getElementById('editFlowDesc').value = editingNode?.description || '';
        setDescEditMode(true);
        document.getElementById('editFlowDesc').focus();
    });
    document.getElementById('btnCancelDesc').addEventListener('click', () => {
        document.getElementById('editFlowDesc').value = editingNode?.description || '';
        setDescEditMode(false);
    });
    document.getElementById('btnSaveDesc').addEventListener('click', async () => {
        const description = document.getElementById('editFlowDesc').value;
        try {
            const res = await fetch('/api/analysis/nodes/' + activeNodeId, {
                method: 'PUT',
                credentials: 'same-origin',
                headers: csrfHeaders(),
                body: JSON.stringify({ description: description })
            });
            const data = await readJsonResponse(res);
            if (!res.ok) {
                throw new Error(data.error || '保存失败');
            }
            treeData = data;
            editingNode = findNodeById(treeData, activeNodeId) || editingNode;
            if (editingNode) {
                editingNode.description = description;
                editingNode.hasDescription = !!(description && description.trim());
            }
            refreshEditViews();
            renderCurrentView();
            showToast('功能描述已保存');
        } catch (e) {
            alert(e.message || '保存失败');
        }
    });

    async function deleteNode(node) {
        if (treeData && node.id === treeData.id) {
            alert('根节点不能删除');
            return;
        }
        const hasChildren = node.children && node.children.length > 0;
        const msg = hasChildren
            ? '确定删除「' + node.title + '」及其下全部流程？\n（逻辑删除，可后续恢复）'
            : '确定删除「' + node.title + '」？\n（逻辑删除，可后续恢复）';
        if (!confirm(msg)) return;

        try {
            const res = await fetch('/api/analysis/nodes/' + node.id, {
                method: 'DELETE',
                credentials: 'same-origin',
                headers: csrfHeaders()
            });
            const data = await readJsonResponse(res);
            if (!res.ok) {
                throw new Error(data.error || '删除失败');
            }
            treeData = data;
            renderCurrentView();
            showToast('已删除（可恢复）');
        } catch (e) {
            alert(e.message || '删除失败');
        }
    }

    if (menuPopup) {
        menuPopup.addEventListener('click', (e) => {
            const btn = e.target.closest('[data-action]');
            if (!btn || !popupNode) return;
            const action = btn.getAttribute('data-action');
            const node = popupNode;
            hideMenuPopup();
            if (action === 'tickets') {
                window.location.href = ticketListUrl(node.title);
            } else if (action === 'edit') {
                openEdit(node);
            } else if (action === 'history') {
                openChangeLog(node);
            } else if (action === 'delete') {
                deleteNode(node);
            }
        });
    }

    document.addEventListener('click', (e) => {
        if (!menuPopup || menuPopup.classList.contains('d-none')) return;
        if (menuPopup.contains(e.target)) return;
        if (e.target.closest('.flow-menu-trigger, .flow-card-title-link')) return;
        hideMenuPopup();
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') hideMenuPopup();
    });

    window.addEventListener('scroll', hideMenuPopup, true);

    document.getElementById('btnCopyFlow').addEventListener('click', async () => {
        try {
            const res = await fetch('/api/analysis/' + projectId + '/text');
            if (!res.ok) throw new Error('获取文本失败');
            const text = await res.text();
            if (navigator.clipboard && navigator.clipboard.writeText) {
                await navigator.clipboard.writeText(text);
                showToast('已复制到剪贴板');
            } else {
                document.getElementById('copyFallbackText').value = text;
                copyFallbackModal.show();
            }
        } catch (e) {
            alert(e.message || '复制失败');
        }
    });

    if (btnViewTree) {
        btnViewTree.addEventListener('click', () => setView('tree'));
    }
    if (btnViewTable) {
        btnViewTable.addEventListener('click', () => setView('table'));
    }

    window.addEventListener('resize', () => {
        hideMenuPopup();
        if (currentView === 'tree') {
            requestAnimationFrame(drawLines);
        }
    });

    updateViewButtons();

    loadTree()
        .then(() => {
            if (loadingEl) loadingEl.remove();
            setView(currentView);
        })
        .catch((e) => {
            if (loadingEl) loadingEl.textContent = '加载失败：' + e.message;
        });
})();
