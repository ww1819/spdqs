(function () {
    'use strict';

    const archiveId = document.querySelector('meta[name="archive-id"]')?.content;
    const toastEl = document.getElementById('nodeToast');
    const tableBody = document.getElementById('nodeTableBody');
    const tableEmpty = document.getElementById('nodeTableEmpty');
    const timelineEmpty = document.getElementById('nodeTimelineEmpty');
    const timelineAxis = document.getElementById('projectTimelineAxis');
    const timelineTrack = document.getElementById('projectTimelineTrack');
    const countLabel = document.getElementById('nodeCountLabel');
    const endDateWrap = document.getElementById('endDateWrap');
    const startDateLabel = document.getElementById('startDateLabel');
    const stageQuick = document.getElementById('nodeStageQuick');
    const nodeStageSelect = document.getElementById('nodeStage');
    const stageManageBody = document.getElementById('stageManageBody');
    const nodeModal = new bootstrap.Modal(document.getElementById('nodeModal'));
    const stageManageModal = new bootstrap.Modal(document.getElementById('stageManageModal'));

    let nodes = [];
    let stages = [];
    let editingId = null;

    function csrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        const headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
        if (token && header) headers[header] = token;
        return headers;
    }

    function showToast(msg) {
        if (!toastEl) return;
        toastEl.textContent = msg;
        toastEl.classList.add('show');
        setTimeout(() => toastEl.classList.remove('show'), 2500);
    }

    async function readJson(res) {
        const text = await res.text();
        const contentType = res.headers.get('content-type') || '';
        if (!contentType.includes('application/json')) {
            throw new Error(text.includes('<html') ? '接口异常，请重新登录后再试' : (text || '请求失败'));
        }
        return JSON.parse(text);
    }

    function escapeHtml(text) {
        return String(text == null ? '' : text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function statusClass(label) {
        if (label === '进行中' || label === '今天') return 'tag-warning';
        if (label === '已结束' || label === '已过') return 'tag-default';
        if (label === '未开始' || label === '未到达') return 'tag-info';
        return 'tag-default';
    }

    function stageClass(stageName) {
        const found = stages.find((s) => s.name === stageName);
        const key = found?.colorKey || '';
        const map = {
            business: 'stage-business',
            research: 'stage-research',
            launch: 'stage-launch',
            warranty: 'stage-warranty',
            maint: 'stage-maint',
            other: 'stage-other'
        };
        if (map[key]) return map[key];
        switch (stageName) {
            case '商务阶段': return 'stage-business';
            case '调研阶段': return 'stage-research';
            case '上线阶段': return 'stage-launch';
            case '质保阶段': return 'stage-warranty';
            case '维保阶段': return 'stage-maint';
            default: return 'stage-other';
        }
    }

    function selectedNodeType() {
        return document.querySelector('input[name="nodeType"]:checked')?.value || '时间点';
    }

    function syncTypeUi() {
        const isRange = selectedNodeType() === '时间段';
        endDateWrap.classList.toggle('d-none', !isRange);
        startDateLabel.innerHTML = isRange
            ? '开始日期 <span class="text-danger">*</span>'
            : '日期 <span class="text-danger">*</span>';
        if (!isRange) {
            document.getElementById('nodeEndDate').value = '';
        }
    }

    function renderStageOptions(selected) {
        if (!nodeStageSelect) return;
        nodeStageSelect.innerHTML = stages.map((s) =>
            '<option value="' + escapeHtml(s.name) + '"' +
            (selected === s.name ? ' selected' : '') + '>' +
            escapeHtml(s.name) + '</option>'
        ).join('');
    }

    function renderStageQuick() {
        if (!stageQuick) return;
        const label = '<span class="text-muted small me-2">快捷添加：</span>';
        if (!stages.length) {
            stageQuick.innerHTML = label + '<span class="text-muted small">暂无阶段，请先点击「维护阶段」添加</span>';
            return;
        }
        stageQuick.innerHTML = label + stages.map((s) =>
            '<button type="button" class="btn btn-outline-secondary btn-sm node-quick-btn" data-stage="' +
            escapeHtml(s.name) + '">' + escapeHtml(s.name) + '</button>'
        ).join('');
        stageQuick.querySelectorAll('.node-quick-btn').forEach((btn) => {
            btn.addEventListener('click', () => openCreate(btn.dataset.stage));
        });
    }

    function renderStageManage() {
        const emptyEl = document.getElementById('stageManageEmpty');
        if (!stages.length) {
            stageManageBody.innerHTML = '';
            emptyEl?.classList.remove('d-none');
            document.getElementById('stageManageTable')?.classList.add('d-none');
            return;
        }
        emptyEl?.classList.add('d-none');
        document.getElementById('stageManageTable')?.classList.remove('d-none');
        stageManageBody.innerHTML = stages.map((s) => `
            <tr data-id="${escapeHtml(s.id)}" class="stage-drag-row" draggable="true">
                <td class="stage-drag-handle" title="拖拽排序">⋮⋮</td>
                <td>
                    <span class="stage-sort-view">${escapeHtml(s.sortOrder)}</span>
                    <input type="number" class="form-control form-control-sm stage-sort-edit d-none"
                           min="0" step="any" value="${escapeHtml(s.sortOrder)}" style="width: 90px;"
                           title="整数=直接设置；小数如 1.55=插入到 1 与 2 之间">
                </td>
                <td>
                    <span class="stage-name-view">${escapeHtml(s.name)}</span>
                    <input type="text" class="form-control form-control-sm stage-name-edit d-none" value="${escapeHtml(s.name)}">
                </td>
                <td class="cell-actions">
                    <button type="button" class="btn btn-link btn-sm p-0 btn-stage-edit">修改</button>
                    <button type="button" class="btn btn-link btn-sm p-0 btn-stage-save d-none">保存</button>
                    <button type="button" class="btn btn-link btn-sm p-0 btn-stage-cancel d-none">取消</button>
                    <button type="button" class="btn btn-link btn-sm text-danger p-0 btn-stage-del">删除</button>
                </td>
            </tr>
        `).join('');
        const nextSort = stages.reduce((max, s) => Math.max(max, Number(s.sortOrder) || 0), 0) + 1;
        const sortInput = document.getElementById('newStageSort');
        if (sortInput && !sortInput.value) {
            sortInput.value = String(nextSort);
        }
        bindStageDragSort();
    }

    let dragStageId = null;

    function bindStageDragSort() {
        if (!stageManageBody) return;
        stageManageBody.querySelectorAll('tr.stage-drag-row').forEach((row) => {
            row.addEventListener('dragstart', (e) => {
                if (row.querySelector('.stage-name-edit:not(.d-none)')) {
                    e.preventDefault();
                    return;
                }
                dragStageId = row.dataset.id;
                row.classList.add('stage-dragging');
                e.dataTransfer.effectAllowed = 'move';
                e.dataTransfer.setData('text/plain', dragStageId);
            });
            row.addEventListener('dragend', () => {
                row.classList.remove('stage-dragging');
                stageManageBody.querySelectorAll('.stage-drag-over').forEach((el) => {
                    el.classList.remove('stage-drag-over');
                });
                dragStageId = null;
            });
            row.addEventListener('dragover', (e) => {
                e.preventDefault();
                e.dataTransfer.dropEffect = 'move';
                const target = e.currentTarget;
                if (!dragStageId || target.dataset.id === dragStageId) return;
                stageManageBody.querySelectorAll('.stage-drag-over').forEach((el) => {
                    el.classList.remove('stage-drag-over');
                });
                target.classList.add('stage-drag-over');
            });
            row.addEventListener('dragleave', (e) => {
                e.currentTarget.classList.remove('stage-drag-over');
            });
            row.addEventListener('drop', async (e) => {
                e.preventDefault();
                const target = e.currentTarget;
                target.classList.remove('stage-drag-over');
                const fromId = dragStageId || e.dataTransfer.getData('text/plain');
                const toId = target.dataset.id;
                if (!fromId || !toId || fromId === toId) return;

                const fromIndex = stages.findIndex((s) => s.id === fromId);
                const toIndex = stages.findIndex((s) => s.id === toId);
                if (fromIndex < 0 || toIndex < 0) return;

                const moved = stages.splice(fromIndex, 1)[0];
                stages.splice(toIndex, 0, moved);
                try {
                    await persistStageOrder();
                } catch (err) {
                    alert(err.message || '排序保存失败');
                    await loadStages();
                }
            });
        });
    }

    async function persistStageOrder() {
        const orderedIds = stages.map((s) => s.id);
        const res = await fetch('/api/archive-node-stages/reorder', {
            method: 'PUT',
            credentials: 'same-origin',
            headers: csrfHeaders(),
            body: JSON.stringify(orderedIds)
        });
        const data = await readJson(res);
        if (!res.ok) throw new Error(data.error || '排序保存失败');
        stages = data;
        refreshStageUi();
        showToast('排序已保存');
    }

    function refreshStageUi(selected) {
        renderStageOptions(selected || stages[0]?.name);
        renderStageQuick();
        renderStageManage();
    }

    async function loadStages() {
        const res = await fetch('/api/archive-node-stages', {
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' }
        });
        const data = await readJson(res);
        if (!res.ok) throw new Error(data.error || '加载阶段失败');
        stages = data;
        refreshStageUi();
    }

    function openCreate(stage) {
        if (!stages.length) {
            alert('请先维护阶段名称');
            stageManageModal.show();
            return;
        }
        editingId = null;
        document.getElementById('nodeModalTitle').textContent = '新增节点';
        document.getElementById('nodeId').value = '';
        renderStageOptions(stage || stages[0].name);
        document.getElementById('nodeTitle').value = stage || stages[0].name || '';
        document.querySelector('input[name="nodeType"][value="时间点"]').checked = true;
        document.getElementById('nodeStartDate').value = '';
        document.getElementById('nodeEndDate').value = '';
        document.getElementById('nodeRemark').value = '';
        syncTypeUi();
        nodeModal.show();
    }

    function openEdit(node) {
        editingId = node.id;
        document.getElementById('nodeModalTitle').textContent = '编辑节点';
        document.getElementById('nodeId').value = node.id;
        const stageNames = stages.map((s) => s.name);
        if (node.stage && !stageNames.includes(node.stage)) {
            // 兼容历史阶段名
            nodeStageSelect.innerHTML =
                '<option value="' + escapeHtml(node.stage) + '">' + escapeHtml(node.stage) + '（历史）</option>' +
                stages.map((s) => '<option value="' + escapeHtml(s.name) + '">' + escapeHtml(s.name) + '</option>').join('');
            nodeStageSelect.value = node.stage;
        } else {
            renderStageOptions(node.stage);
        }
        document.getElementById('nodeTitle').value = node.title || '';
        const type = node.range ? '时间段' : '时间点';
        document.querySelector('input[name="nodeType"][value="' + type + '"]').checked = true;
        document.getElementById('nodeStartDate').value = node.startDate || '';
        document.getElementById('nodeEndDate').value = node.endDate || '';
        document.getElementById('nodeRemark').value = node.remark || '';
        syncTypeUi();
        nodeModal.show();
    }

    function renderTable() {
        if (!tableBody) return;
        if (!nodes.length) {
            tableBody.innerHTML = '';
            tableEmpty?.classList.remove('d-none');
            document.getElementById('nodeTable')?.classList.add('d-none');
            return;
        }
        tableEmpty?.classList.add('d-none');
        document.getElementById('nodeTable')?.classList.remove('d-none');
        tableBody.innerHTML = nodes.map((n) => `
            <tr>
                <td><span class="tag ${stageClass(n.stage)}">${escapeHtml(n.stage)}</span></td>
                <td>${escapeHtml(n.title)}</td>
                <td>${escapeHtml(n.nodeType)}</td>
                <td>${escapeHtml(n.dateLabel)}</td>
                <td><span class="tag ${statusClass(n.statusLabel)}">${escapeHtml(n.statusLabel)}</span></td>
                <td class="cell-clamp">${escapeHtml(n.remark || '—')}</td>
                <td class="cell-actions">
                    <button type="button" class="btn btn-link btn-sm p-0" data-edit="${n.id}">编辑</button>
                    <button type="button" class="btn btn-link btn-sm text-danger p-0" data-del="${n.id}">删除</button>
                </td>
            </tr>
        `).join('');
    }

    function renderTimeline() {
        if (!timelineAxis || !timelineTrack) return;
        if (!nodes.length) {
            timelineEmpty?.classList.remove('d-none');
            timelineAxis.innerHTML = '';
            timelineTrack.innerHTML = '';
            return;
        }
        timelineEmpty?.classList.add('d-none');

        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const dates = [];
        nodes.forEach((n) => {
            if (n.startDate) dates.push(new Date(n.startDate));
            if (n.endDate) dates.push(new Date(n.endDate));
        });
        dates.push(today);
        let min = new Date(Math.min(...dates));
        let max = new Date(Math.max(...dates));
        if (min.getTime() === max.getTime()) {
            min = new Date(min.getTime() - 7 * 86400000);
            max = new Date(max.getTime() + 7 * 86400000);
        } else {
            const pad = Math.max(3, Math.round((max - min) / 86400000 * 0.08));
            min = new Date(min.getTime() - pad * 86400000);
            max = new Date(max.getTime() + pad * 86400000);
        }
        const span = max.getTime() - min.getTime();

        function pct(dateStr) {
            const d = new Date(dateStr);
            return ((d.getTime() - min.getTime()) / span) * 100;
        }

        const ticks = [];
        const tickCount = 6;
        for (let i = 0; i <= tickCount; i++) {
            ticks.push(new Date(min.getTime() + (span * i) / tickCount));
        }
        timelineAxis.innerHTML = ticks.map((t) => {
            const left = ((t.getTime() - min.getTime()) / span) * 100;
            const label = t.getFullYear() + '-' + String(t.getMonth() + 1).padStart(2, '0') + '-' + String(t.getDate()).padStart(2, '0');
            return `<div class="timeline-tick" style="left:${left}%"><span>${label}</span></div>`;
        }).join('');

        const todayPct = ((today.getTime() - min.getTime()) / span) * 100;
        let html = `<div class="timeline-today" style="left:${todayPct}%" title="今天"></div>`;

        nodes.forEach((n, idx) => {
            const lane = idx % 3;
            if (n.range && n.endDate) {
                const left = pct(n.startDate);
                const right = pct(n.endDate);
                const width = Math.max(1.2, right - left);
                html += `<div class="timeline-range ${stageClass(n.stage)}" style="left:${left}%;width:${width}%;top:${28 + lane * 34}px"
                    title="${escapeHtml(n.title)} (${escapeHtml(n.dateLabel)})">
                    <span>${escapeHtml(n.title)}</span>
                </div>`;
            } else {
                const left = pct(n.startDate);
                html += `<div class="timeline-point ${stageClass(n.stage)}" style="left:${left}%;top:${28 + lane * 34}px"
                    title="${escapeHtml(n.title)} (${escapeHtml(n.dateLabel)})">
                    <i></i><span>${escapeHtml(n.title)}</span>
                </div>`;
            }
        });
        timelineTrack.innerHTML = html;
        timelineTrack.style.minHeight = (28 + Math.min(nodes.length, 3) * 34 + 24) + 'px';
    }

    function renderAll() {
        if (countLabel) countLabel.textContent = '共 ' + nodes.length + ' 个节点';
        renderTable();
        renderTimeline();
    }

    async function loadNodes() {
        const res = await fetch('/api/archives/' + encodeURIComponent(archiveId) + '/nodes', {
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' }
        });
        const data = await readJson(res);
        if (!res.ok) throw new Error(data.error || '加载失败');
        nodes = data;
        renderAll();
    }

    async function saveNode() {
        const payload = {
            stage: document.getElementById('nodeStage').value,
            title: document.getElementById('nodeTitle').value.trim(),
            nodeType: selectedNodeType(),
            startDate: document.getElementById('nodeStartDate').value,
            endDate: document.getElementById('nodeEndDate').value || null,
            remark: document.getElementById('nodeRemark').value
        };
        if (!payload.stage) {
            alert('请选择阶段');
            return;
        }
        if (!payload.startDate) {
            alert('请填写日期');
            return;
        }
        if (payload.nodeType === '时间段' && !payload.endDate) {
            alert('时间段须填写结束日期');
            return;
        }
        const url = editingId
            ? '/api/archives/' + encodeURIComponent(archiveId) + '/nodes/' + encodeURIComponent(editingId)
            : '/api/archives/' + encodeURIComponent(archiveId) + '/nodes';
        const method = editingId ? 'PUT' : 'POST';
        try {
            const res = await fetch(url, {
                method,
                credentials: 'same-origin',
                headers: csrfHeaders(),
                body: JSON.stringify(payload)
            });
            const data = await readJson(res);
            if (!res.ok) throw new Error(data.error || '保存失败');
            nodes = data;
            nodeModal.hide();
            renderAll();
            showToast(editingId ? '节点已更新' : '节点已创建');
        } catch (e) {
            alert(e.message || '保存失败');
        }
    }

    async function deleteNode(id) {
        const node = nodes.find((n) => n.id === id);
        if (!confirm('确定删除节点「' + (node?.title || '') + '」？')) return;
        try {
            const res = await fetch('/api/archives/' + encodeURIComponent(archiveId) + '/nodes/' + encodeURIComponent(id), {
                method: 'DELETE',
                credentials: 'same-origin',
                headers: csrfHeaders()
            });
            const data = await readJson(res);
            if (!res.ok) throw new Error(data.error || '删除失败');
            nodes = data;
            renderAll();
            showToast('已删除');
        } catch (e) {
            alert(e.message || '删除失败');
        }
    }

    async function addStage() {
        const name = document.getElementById('newStageName').value.trim();
        const sortRaw = document.getElementById('newStageSort').value;
        const sortOrder = sortRaw === '' ? undefined : Number(sortRaw);
        if (!name) {
            alert('请输入阶段名称');
            return;
        }
        if (sortRaw !== '' && (!Number.isFinite(sortOrder) || sortOrder < 0)) {
            alert('请输入有效序号（≥0；可用小数如 1.55 表示插入）');
            return;
        }
        try {
            const res = await fetch('/api/archive-node-stages', {
                method: 'POST',
                credentials: 'same-origin',
                headers: csrfHeaders(),
                body: JSON.stringify({
                    name: name,
                    sortOrder: sortOrder
                })
            });
            const data = await readJson(res);
            if (!res.ok) throw new Error(data.error || '新增失败');
            stages = data;
            document.getElementById('newStageName').value = '';
            document.getElementById('newStageSort').value = '';
            refreshStageUi();
            showToast('阶段已新增');
        } catch (e) {
            alert(e.message || '新增失败');
        }
    }

    async function saveStage(id, name, sortOrder) {
        const res = await fetch('/api/archive-node-stages/' + encodeURIComponent(id), {
            method: 'PUT',
            credentials: 'same-origin',
            headers: csrfHeaders(),
            body: JSON.stringify({ name: name, sortOrder: sortOrder })
        });
        const data = await readJson(res);
        if (!res.ok) throw new Error(data.error || '保存失败');
        stages = data;
        refreshStageUi();
        await loadNodes();
        showToast('阶段已更新');
    }

    async function deleteStage(id) {
        const stage = stages.find((s) => s.id === id);
        if (!confirm('确定删除阶段「' + (stage?.name || '') + '」？')) return;
        try {
            const res = await fetch('/api/archive-node-stages/' + encodeURIComponent(id), {
                method: 'DELETE',
                credentials: 'same-origin',
                headers: csrfHeaders()
            });
            const data = await readJson(res);
            if (!res.ok) throw new Error(data.error || '删除失败');
            stages = data;
            refreshStageUi();
            showToast('阶段已删除');
        } catch (e) {
            alert(e.message || '删除失败');
        }
    }

    document.querySelectorAll('input[name="nodeType"]').forEach((el) => {
        el.addEventListener('change', syncTypeUi);
    });
    document.getElementById('btnAddNode')?.addEventListener('click', () => openCreate());
    document.getElementById('btnAddNodeEmpty')?.addEventListener('click', () => openCreate());
    document.getElementById('btnSaveNode')?.addEventListener('click', saveNode);
    document.getElementById('btnManageStages')?.addEventListener('click', () => {
        renderStageManage();
        stageManageModal.show();
    });
    document.getElementById('btnAddStage')?.addEventListener('click', addStage);
    document.getElementById('newStageName')?.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            addStage();
        }
    });

    stageManageBody?.addEventListener('click', async (e) => {
        const tr = e.target.closest('tr[data-id]');
        if (!tr) return;
        const id = tr.dataset.id;
        const nameView = tr.querySelector('.stage-name-view');
        const nameEdit = tr.querySelector('.stage-name-edit');
        const sortView = tr.querySelector('.stage-sort-view');
        const sortEdit = tr.querySelector('.stage-sort-edit');
        const btnEdit = tr.querySelector('.btn-stage-edit');
        const btnSave = tr.querySelector('.btn-stage-save');
        const btnCancel = tr.querySelector('.btn-stage-cancel');
        const btnDel = tr.querySelector('.btn-stage-del');

        if (e.target.classList.contains('btn-stage-edit')) {
            nameView.classList.add('d-none');
            nameEdit.classList.remove('d-none');
            sortView.classList.add('d-none');
            sortEdit.classList.remove('d-none');
            btnEdit.classList.add('d-none');
            btnSave.classList.remove('d-none');
            btnCancel.classList.remove('d-none');
            btnDel.classList.add('d-none');
            sortEdit.focus();
            return;
        }
        if (e.target.classList.contains('btn-stage-cancel')) {
            nameEdit.value = nameView.textContent;
            sortEdit.value = sortView.textContent;
            nameView.classList.remove('d-none');
            nameEdit.classList.add('d-none');
            sortView.classList.remove('d-none');
            sortEdit.classList.add('d-none');
            btnEdit.classList.remove('d-none');
            btnSave.classList.add('d-none');
            btnCancel.classList.add('d-none');
            btnDel.classList.remove('d-none');
            return;
        }
        if (e.target.classList.contains('btn-stage-save')) {
            const name = nameEdit.value.trim();
            const sortOrder = Number(sortEdit.value);
            if (!name) {
                alert('阶段名称不能为空');
                return;
            }
            if (!Number.isFinite(sortOrder) || sortOrder < 0) {
                alert('请输入有效序号（≥0；可用小数如 1.55 表示插入）');
                return;
            }
            try {
                await saveStage(id, name, sortOrder);
            } catch (err) {
                alert(err.message || '保存失败');
            }
            return;
        }
        if (e.target.classList.contains('btn-stage-del')) {
            deleteStage(id);
        }
    });

    tableBody?.addEventListener('click', (e) => {
        const editId = e.target.getAttribute('data-edit');
        const delId = e.target.getAttribute('data-del');
        if (editId) {
            const node = nodes.find((n) => n.id === editId);
            if (node) openEdit(node);
        } else if (delId) {
            deleteNode(delId);
        }
    });

    syncTypeUi();
    Promise.all([loadStages(), loadNodes()])
        .catch((e) => {
            if (countLabel) countLabel.textContent = '加载失败';
            alert(e.message || '加载失败');
        });
})();
