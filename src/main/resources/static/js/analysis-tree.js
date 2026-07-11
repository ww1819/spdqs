(function () {
    'use strict';

    const wrapEl = document.getElementById('flowCanvasWrap');
    const projectId = wrapEl?.dataset.projectId;
    const canvas = document.getElementById('flowCanvas');
    const linesSvg = document.getElementById('flowLines');
    const loadingEl = document.getElementById('flowLoading');
    const toastEl = document.getElementById('flowToast');

    const newFlowModal = new bootstrap.Modal(document.getElementById('newFlowModal'));
    const descModal = new bootstrap.Modal(document.getElementById('descModal'));
    const copyFallbackModal = new bootstrap.Modal(document.getElementById('copyFallbackModal'));

    let treeData = null;
    let pendingParentId = null;
    let activeNodeId = null;
    let descEditing = false;

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
        renderTree();
    }

    function renderTree() {
        if (!treeData) return;
        canvas.innerHTML = '';
        const root = document.createElement('div');
        root.className = 'flow-tree-root';
        root.appendChild(renderNode(treeData));
        canvas.appendChild(root);
        requestAnimationFrame(() => requestAnimationFrame(drawLines));
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
            '<button type="button" class="flow-tool-btn" data-action="desc">功能说明</button>' +
            '<button type="button" class="flow-tool-btn flow-tool-danger" data-action="delete">删除功能</button>';

        const body = document.createElement('div');
        body.className = 'flow-card-body';
        const title = document.createElement('span');
        title.className = 'flow-card-title';
        title.textContent = node.title;
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
        toolbar.querySelector('[data-action="desc"]').addEventListener('click', (e) => {
            e.stopPropagation();
            openDesc(node);
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

    function getCardElement(nodeWrap) {
        return nodeWrap.querySelector(':scope > .flow-card-zone > .flow-card');
    }

    function addLine(linesSvg, x1, y1, x2, y2) {
        const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
        line.setAttribute('x1', x1);
        line.setAttribute('y1', y1);
        line.setAttribute('x2', x2);
        line.setAttribute('y2', y2);
        line.setAttribute('class', 'flow-line');
        linesSvg.appendChild(line);
    }

    function drawLines() {
        if (!linesSvg || !canvas) return;
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
            renderTree();
            showToast('流程已创建');
        } catch (e) {
            alert(e.message || '创建失败');
        }
    });

    function openDesc(node) {
        activeNodeId = node.id;
        descEditing = !node.hasDescription;
        document.getElementById('descModalTitle').textContent = node.title + ' — 功能说明';

        const viewEl = document.getElementById('descView');
        const editEl = document.getElementById('descEdit');
        const btnEdit = document.getElementById('btnDescEdit');
        const btnSave = document.getElementById('btnDescSave');

        if (node.hasDescription && !descEditing) {
            viewEl.textContent = node.description;
            viewEl.classList.remove('d-none');
            editEl.classList.add('d-none');
            btnEdit.classList.remove('d-none');
            btnSave.classList.add('d-none');
        } else {
            editEl.value = node.description || '';
            viewEl.classList.add('d-none');
            editEl.classList.remove('d-none');
            btnEdit.classList.add('d-none');
            btnSave.classList.remove('d-none');
        }

        btnEdit.onclick = () => {
            descEditing = true;
            editEl.value = node.description || '';
            viewEl.classList.add('d-none');
            editEl.classList.remove('d-none');
            btnEdit.classList.add('d-none');
            btnSave.classList.remove('d-none');
            editEl.focus();
        };

        descModal.show();
    }

    document.getElementById('btnDescSave').addEventListener('click', async () => {
        const description = document.getElementById('descEdit').value;
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
            descModal.hide();
            renderTree();
            showToast('功能说明已保存');
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
            ? '确定删除「' + node.title + '」及其下全部流程？'
            : '确定删除「' + node.title + '」？';
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
            renderTree();
            showToast('已删除');
        } catch (e) {
            alert(e.message || '删除失败');
        }
    }

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

    window.addEventListener('resize', () => requestAnimationFrame(drawLines));

    loadTree()
        .then(() => {
            if (loadingEl) loadingEl.remove();
        })
        .catch((e) => {
            if (loadingEl) loadingEl.textContent = '加载失败：' + e.message;
        });
})();
