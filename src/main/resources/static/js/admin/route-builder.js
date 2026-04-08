// ============================================================
// DOCFLOW — Визуальный конструктор маршрутов согласования
// ============================================================

const RouteBuilder = {

  // --- Состояние ---
  nodes: [],          // Массив узлов на канвасе
  connections: [],    // Массив соединений между узлами
  selectedNode: null, // Выбранный узел
  connectingFrom: null, // Узел из которого тянем стрелку
  nextNodeId: 1,

  // --- Данные из API ---
  _roles: [],
  _departments: [],
  _users: [],
  _docTypes: [],
  _templates: [],
  _currentTemplateId: null,

  // ============================================================
  // ИНИЦИАЛИЗАЦИЯ
  // ============================================================
  async init() {
    await this.loadData();
    this.renderPalette();
    this.renderTemplateList();
    this.initCanvas();
  },

  async loadData() {
    const [roles, depts, users, docTypes, templates] = await Promise.all([
      API.roles.getAll().catch(() => []),
      API.departments.getAll().catch(() => []),
      API.users.getAll().catch(() => []),
      API.documentTypes.getAll().catch(() => []),
      API.routeTemplates.getAll().catch(() => []),
    ]);
    this._roles = roles;
    this._departments = depts;
    this._users = users;
    this._docTypes = docTypes;
    this._templates = templates;
  },

  // ============================================================
  // ПАЛИТРА ЭЛЕМЕНТОВ
  // ============================================================
  renderPalette() {
    const palette = document.getElementById('palette-items');
    if (!palette) return;

    const stepTypes = [
      { type: 'APPROVAL',   label: 'Согласование', icon: 'bi-check2' },
      { type: 'SIGNATURE',  label: 'Подпись',       icon: 'bi-pen' },
      { type: 'EXECUTION',  label: 'Исполнение',    icon: 'bi-gear' },
      { type: 'RESOLUTION', label: 'Резолюция',     icon: 'bi-chat-left-dots' },
    ];

    palette.innerHTML = `
      <div class="palette-section">
        <div class="palette-section-title">Типы шагов</div>
        ${stepTypes.map(st => `
          <div class="palette-item" draggable="true"
               data-step-type="${st.type}"
               ondragstart="RouteBuilder.onPaletteDragStart(event)">
            <i class="bi ${st.icon}"></i>${st.label}
          </div>
        `).join('')}
      </div>
    `;
  },

  // ============================================================
  // СПИСОК ШАБЛОНОВ
  // ============================================================
  renderTemplateList() {
    const list = document.getElementById('template-list');
    if (!list) return;

    if (!this._templates.length) {
      list.innerHTML = '<p class="text-muted" style="font-size:0.82rem;padding:0.5rem">Нет шаблонов маршрутов</p>';
      return;
    }

    list.innerHTML = this._templates.map(t => {
      const docType = this._docTypes.find(d => d.id === t.documentTypeId);
      return `
        <div class="template-item" onclick="RouteBuilder.loadTemplate(${t.id})">
          <div>
            <div class="template-item-name">${t.name}</div>
            <div class="template-item-type">${docType?.displayName || 'Тип не задан'}</div>
          </div>
          <div class="template-item-actions">
            <button class="btn btn-ghost btn-sm btn-icon" onclick="event.stopPropagation(); RouteBuilder.deleteTemplate(${t.id})" title="Удалить">
              <i class="bi bi-trash"></i>
            </button>
          </div>
        </div>
      `;
    }).join('');
  },

  // ============================================================
  // КАНВАС
  // ============================================================
  initCanvas() {
    const canvas = document.getElementById('builder-canvas');
    if (!canvas) return;

    // Drag over для сброса с палитры
    canvas.addEventListener('dragover', e => e.preventDefault());
    canvas.addEventListener('drop', e => this.onCanvasDrop(e));

    // Клик по канвасу — снять выделение
    canvas.addEventListener('click', e => {
      if (e.target === canvas || e.target.id === 'builder-canvas') {
        this.selectNode(null);
        if (this.connectingFrom) {
          this.connectingFrom = null;
          this.updateConnectingUI();
        }
      }
    });

    this.redrawSVG();
  },

  // Drag start с палитры
  onPaletteDragStart(event) {
    event.dataTransfer.setData('stepType', event.currentTarget.dataset.stepType);
    event.dataTransfer.setData('source', 'palette');
  },

  // Drop на канвас
  onCanvasDrop(event) {
    event.preventDefault();
    const source = event.dataTransfer.getData('source');
    if (source !== 'palette') return;

    const stepType = event.dataTransfer.getData('stepType');
    const canvas = document.getElementById('builder-canvas');
    const rect = canvas.getBoundingClientRect();
    const x = event.clientX - rect.left - 85;
    const y = event.clientY - rect.top - 40;

    this.addNode(stepType, Math.max(20, x), Math.max(20, y));
  },

  // ============================================================
  // УЗЛЫ
  // ============================================================
  addNode(stepType, x, y) {
    const node = {
      id: this.nextNodeId++,
      stepType,
      x, y,
      name: this.getDefaultStepName(stepType),
      assignedRoleId: null,
      assignedDepartmentId: null,
      assignedUserId: null,
    };
    this.nodes.push(node);
    this.renderNode(node);
    this.selectNode(node.id);
  },

  getDefaultStepName(stepType) {
    const names = {
      APPROVAL:   'Согласование',
      SIGNATURE:  'Подпись',
      EXECUTION:  'Исполнение',
      RESOLUTION: 'Резолюция',
    };
    return names[stepType] || 'Шаг';
  },

  renderNode(node) {
    const canvas = document.getElementById('builder-canvas');
    if (!canvas) return;

    const existing = document.getElementById(`node-${node.id}`);
    if (existing) existing.remove();

    const assigneeName = this.getAssigneeName(node);
    const typeClass = node.stepType.toLowerCase();

    const div = document.createElement('div');
    div.className = 'route-node';
    div.id = `node-${node.id}`;
    div.style.left = node.x + 'px';
    div.style.top  = node.y + 'px';

    div.innerHTML = `
      <div class="node-header">
        <span class="node-type-badge ${typeClass}">${this.getStepTypeLabel(node.stepType)}</span>
        <button class="node-delete" onclick="RouteBuilder.deleteNode(${node.id})" title="Удалить">
          <i class="bi bi-x"></i>
        </button>
      </div>
      <div class="node-body">
        <div class="node-name">${node.name}</div>
        <div class="node-assignee"><i class="bi bi-person"></i>${assigneeName}</div>
      </div>
      <div class="node-port port-in"  onclick="RouteBuilder.onPortClick(event, ${node.id}, 'in')"></div>
      <div class="node-port port-out" onclick="RouteBuilder.onPortClick(event, ${node.id}, 'out')"></div>
    `;

    // Drag для перемещения узла
    this.makeDraggable(div, node);

    // Клик по узлу — выбрать
    div.addEventListener('click', (e) => {
      if (!e.target.classList.contains('node-port') && !e.target.classList.contains('node-delete')) {
        this.selectNode(node.id);
      }
    });

    canvas.appendChild(div);
  },

  getStepTypeLabel(type) {
    const labels = { APPROVAL: 'Согласование', SIGNATURE: 'Подпись', EXECUTION: 'Исполнение', RESOLUTION: 'Резолюция' };
    return labels[type] || type;
  },

  getAssigneeName(node) {
    if (node.assignedUserId) {
      const u = this._users.find(u => u.id === node.assignedUserId);
      return u ? u.fullName : 'Пользователь';
    }
    if (node.assignedDepartmentId) {
      const d = this._departments.find(d => d.id === node.assignedDepartmentId);
      return d ? d.name : 'Отдел';
    }
    if (node.assignedRoleId) {
      const r = this._roles.find(r => r.id === node.assignedRoleId);
      return r ? r.displayName : 'Роль';
    }
    return 'Не задан';
  },

  // Перемещение узла
  makeDraggable(el, node) {
    let dragging = false, startX, startY, startLeft, startTop;

    el.addEventListener('mousedown', (e) => {
      if (e.target.classList.contains('node-port') || e.target.classList.contains('node-delete') || e.target.tagName === 'BUTTON' || e.target.tagName === 'I') return;
      dragging = true;
      startX = e.clientX;
      startY = e.clientY;
      startLeft = node.x;
      startTop  = node.y;
      el.style.zIndex = 10;
      e.preventDefault();
    });

    document.addEventListener('mousemove', (e) => {
      if (!dragging) return;
      node.x = Math.max(0, startLeft + e.clientX - startX);
      node.y = Math.max(0, startTop  + e.clientY - startY);
      el.style.left = node.x + 'px';
      el.style.top  = node.y + 'px';
      this.redrawSVG();
    });

    document.addEventListener('mouseup', () => {
      if (dragging) {
        dragging = false;
        el.style.zIndex = '';
      }
    });
  },

  // Клик по порту — режим соединения
  onPortClick(event, nodeId, portType) {
    event.stopPropagation();

    if (this.connectingFrom === null) {
      // Начинаем соединение от out-порта
      if (portType === 'out') {
        this.connectingFrom = nodeId;
        this.updateConnectingUI();
      }
    } else {
      // Заканчиваем соединение в in-порту
      if (portType === 'in' && this.connectingFrom !== nodeId) {
        this.addConnection(this.connectingFrom, nodeId);
        this.connectingFrom = null;
        this.updateConnectingUI();
      } else {
        this.connectingFrom = null;
        this.updateConnectingUI();
      }
    }
  },

  addConnection(fromId, toId) {
    // Проверить что такого соединения нет
    const exists = this.connections.find(c => c.from === fromId && c.to === toId);
    if (exists) return;

    this.connections.push({ from: fromId, to: toId });
    this.redrawSVG();
  },

  updateConnectingUI() {
    document.querySelectorAll('.route-node').forEach(el => {
      el.classList.remove('connecting-source');
    });
    if (this.connectingFrom !== null) {
      document.getElementById(`node-${this.connectingFrom}`)?.classList.add('connecting-source');
    }

    const hint = document.getElementById('builder-hint');
    if (hint) {
      hint.textContent = this.connectingFrom !== null
        ? 'Кликните на входящий порт (●) другого блока для соединения'
        : 'Кликните на исходящий порт (●) блока для создания связи';
    }
  },

  deleteNode(nodeId) {
    this.nodes = this.nodes.filter(n => n.id !== nodeId);
    this.connections = this.connections.filter(c => c.from !== nodeId && c.to !== nodeId);
    document.getElementById(`node-${nodeId}`)?.remove();
    if (this.selectedNode === nodeId) this.selectNode(null);
    this.redrawSVG();
  },

  selectNode(nodeId) {
    this.selectedNode = nodeId;

    document.querySelectorAll('.route-node').forEach(el => el.classList.remove('selected'));
    if (nodeId !== null) {
      document.getElementById(`node-${nodeId}`)?.classList.add('selected');
    }

    this.renderProps(nodeId);
  },

  // ============================================================
  // SVG СОЕДИНЕНИЯ
  // ============================================================
  redrawSVG() {
    const svg = document.getElementById('builder-svg');
    if (!svg) return;

    svg.innerHTML = `
      <defs>
        <marker id="arrow" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
          <path d="M0,0 L0,6 L8,3 z" class="connection-arrow"/>
        </marker>
        <marker id="arrow-active" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
          <path d="M0,0 L0,6 L8,3 z" class="connection-arrow active"/>
        </marker>
      </defs>
      ${this.connections.map(c => this.renderConnection(c)).join('')}
    `;
  },

  renderConnection(conn) {
    const fromNode = this.nodes.find(n => n.id === conn.from);
    const toNode   = this.nodes.find(n => n.id === conn.to);
    if (!fromNode || !toNode) return '';

    const fromEl = document.getElementById(`node-${fromNode.id}`);
    const toEl   = document.getElementById(`node-${toNode.id}`);
    if (!fromEl || !toEl) return '';

    const fw = fromEl.offsetWidth;
    const fh = fromEl.offsetHeight;
    const tw = toEl.offsetWidth;
    const th = toEl.offsetHeight;

    const x1 = fromNode.x + fw;      // Правый край from
    const y1 = fromNode.y + fh / 2;  // Середина по высоте
    const x2 = toNode.x;             // Левый край to
    const y2 = toNode.y + th / 2;

    // Кубическая кривая Безье
    const cx = (x1 + x2) / 2;
    const path = `M ${x1} ${y1} C ${cx} ${y1} ${cx} ${y2} ${x2} ${y2}`;

    // Метка с порядковым номером
    const midX = (x1 + x2) / 2;
    const midY = (y1 + y2) / 2;

    return `
      <path d="${path}" class="connection-line" marker-end="url(#arrow)"
            onclick="RouteBuilder.deleteConnection(${conn.from}, ${conn.to})"
            style="pointer-events: stroke; cursor: pointer;"
            title="Удалить связь"/>
      <circle cx="${midX}" cy="${midY}" r="8" fill="white" stroke="#dde3ed" stroke-width="1"/>
      <text x="${midX}" y="${midY + 1}" text-anchor="middle" dominant-baseline="middle" class="connection-label">
        ${this.getConnectionOrder(conn)}
      </text>
    `;
  },

  getConnectionOrder(conn) {
    return this.connections.indexOf(conn) + 1;
  },

  deleteConnection(fromId, toId) {
    this.connections = this.connections.filter(c => !(c.from === fromId && c.to === toId));
    this.redrawSVG();
  },

  // ============================================================
  // ПАНЕЛЬ СВОЙСТВ
  // ============================================================
  renderProps(nodeId) {
    const body = document.getElementById('props-body');
    if (!body) return;

    if (nodeId === null) {
      body.innerHTML = `<div class="props-empty"><i class="bi bi-cursor"></i>Выберите блок для редактирования</div>`;
      return;
    }

    const node = this.nodes.find(n => n.id === nodeId);
    if (!node) return;

    const roleOptions = this._roles.map(r => `<option value="${r.id}" ${node.assignedRoleId === r.id ? 'selected' : ''}>${r.displayName}</option>`).join('');
    const deptOptions = this._departments.map(d => `<option value="${d.id}" ${node.assignedDepartmentId === d.id ? 'selected' : ''}>${d.name}</option>`).join('');
    const userOptions = this._users.map(u => `<option value="${u.id}" ${node.assignedUserId === u.id ? 'selected' : ''}>${u.fullName}</option>`).join('');

    const assignType = node.assignedUserId ? 'user' : node.assignedDepartmentId ? 'department' : 'role';

    body.innerHTML = `
      <div class="form-group">
        <label class="form-label">Название шага <span class="required">*</span></label>
        <input type="text" class="form-control" value="${node.name}"
               oninput="RouteBuilder.updateNodeName(${node.id}, this.value)">
      </div>
      <div class="form-group">
        <label class="form-label">Тип шага</label>
        <select class="form-control" onchange="RouteBuilder.updateNodeStepType(${node.id}, this.value)">
          <option value="APPROVAL"   ${node.stepType === 'APPROVAL'   ? 'selected' : ''}>Согласование</option>
          <option value="SIGNATURE"  ${node.stepType === 'SIGNATURE'  ? 'selected' : ''}>Подпись</option>
          <option value="EXECUTION"  ${node.stepType === 'EXECUTION'  ? 'selected' : ''}>Исполнение</option>
          <option value="RESOLUTION" ${node.stepType === 'RESOLUTION' ? 'selected' : ''}>Резолюция</option>
        </select>
      </div>
      <div class="divider"></div>
      <div class="form-group">
        <label class="form-label">Назначить</label>
        <select class="form-control" id="assign-type-${node.id}" onchange="RouteBuilder.updateAssignType(${node.id}, this.value)">
          <option value="role"       ${assignType === 'role'       ? 'selected' : ''}>По роли</option>
          <option value="department" ${assignType === 'department' ? 'selected' : ''}>Отделу</option>
          <option value="user"       ${assignType === 'user'       ? 'selected' : ''}>Конкретному сотруднику</option>
        </select>
      </div>
      <div class="form-group" id="assign-role-${node.id}" ${assignType !== 'role' ? 'style="display:none"' : ''}>
        <label class="form-label">Роль <span class="required">*</span></label>
        <select class="form-control" onchange="RouteBuilder.updateNodeProp(${node.id}, 'assignedRoleId', this.value ? Number(this.value) : null)">
          <option value="">— Выберите роль —</option>
          ${roleOptions}
        </select>
      </div>
      <div class="form-group" id="assign-dept-${node.id}" ${assignType !== 'department' ? 'style="display:none"' : ''}>
        <label class="form-label">Отдел <span class="required">*</span></label>
        <select class="form-control" onchange="RouteBuilder.updateNodeProp(${node.id}, 'assignedDepartmentId', this.value ? Number(this.value) : null)">
          <option value="">— Выберите отдел —</option>
          ${deptOptions}
        </select>
      </div>
      <div class="form-group" id="assign-user-${node.id}" ${assignType !== 'user' ? 'style="display:none"' : ''}>
        <label class="form-label">Сотрудник <span class="required">*</span></label>
        <select class="form-control" onchange="RouteBuilder.updateNodeProp(${node.id}, 'assignedUserId', this.value ? Number(this.value) : null)">
          <option value="">— Выберите сотрудника —</option>
          ${userOptions}
        </select>
      </div>
    `;
  },

  updateNodeName(nodeId, value) {
    const node = this.nodes.find(n => n.id === nodeId);
    if (!node) return;
    node.name = value;
    const nameEl = document.querySelector(`#node-${nodeId} .node-name`);
    if (nameEl) nameEl.textContent = value;
  },

  updateNodeStepType(nodeId, value) {
    const node = this.nodes.find(n => n.id === nodeId);
    if (!node) return;
    node.stepType = value;
    this.renderNode(node);
    this.selectNode(nodeId);
    this.redrawSVG();
  },

  updateAssignType(nodeId, type) {
    const node = this.nodes.find(n => n.id === nodeId);
    if (!node) return;

    // Сбрасываем все назначения
    node.assignedRoleId = null;
    node.assignedDepartmentId = null;
    node.assignedUserId = null;

    // Показываем нужный select
    ['role', 'department', 'user'].forEach(t => {
      const el = document.getElementById(`assign-${t}-${nodeId}`);
      if (el) el.style.display = t === type ? '' : 'none';
    });
  },

  updateNodeProp(nodeId, prop, value) {
    const node = this.nodes.find(n => n.id === nodeId);
    if (!node) return;
    node[prop] = value;

    // Обновить отображение исполнителя на карточке
    const assigneeEl = document.querySelector(`#node-${nodeId} .node-assignee`);
    if (assigneeEl) {
      assigneeEl.innerHTML = `<i class="bi bi-person"></i>${this.getAssigneeName(node)}`;
    }
  },

  // ============================================================
  // СОХРАНЕНИЕ / ЗАГРУЗКА ШАБЛОНОВ
  // ============================================================
  openSaveModal() {
    if (!this.nodes.length) {
      showToast('Добавьте хотя бы один шаг', 'error');
      return;
    }

    // Заполнить выпадающий список типов документов
    const docTypeSelect = document.getElementById('save-doc-type');
    if (docTypeSelect) {
      docTypeSelect.innerHTML = '<option value="">— Выберите тип документа —</option>' +
        this._docTypes.map(t => `<option value="${t.id}">${t.displayName}</option>`).join('');
    }

    // Если редактируем существующий — заполнить данными
    if (this._currentTemplateId) {
      const tpl = this._templates.find(t => t.id === this._currentTemplateId);
      if (tpl) {
        document.getElementById('save-name').value = tpl.name;
        if (docTypeSelect) docTypeSelect.value = tpl.documentTypeId;
      }
    } else {
      document.getElementById('save-name').value = '';
    }

    showModal('save-modal');
  },

  async saveTemplate() {
    const name = document.getElementById('save-name').value?.trim();
    const docTypeId = document.getElementById('save-doc-type').value;

    if (!name) { showToast('Введите название шаблона', 'error'); return; }
    if (!docTypeId) { showToast('Выберите тип документа', 'error'); return; }

    // Валидация: у каждого узла должен быть исполнитель
    for (const node of this.nodes) {
      if (!node.assignedRoleId && !node.assignedDepartmentId && !node.assignedUserId) {
        showToast(`Шаг "${node.name}" не имеет исполнителя`, 'error');
        return;
      }
    }

    // Определить порядок шагов по соединениям
    const orderedNodes = this.getOrderedNodes();

    const steps = orderedNodes.map((node, idx) => ({
      stepOrder:            idx + 1,
      stepName:             node.name,
      stepType:             node.stepType,
      assignedRoleId:       node.assignedRoleId,
      assignedDepartmentId: node.assignedDepartmentId,
      assignedUserId:       node.assignedUserId,
    }));

    const data = {
      name,
      documentTypeId: Number(docTypeId),
      active: true,
      steps,
    };

    try {
      if (this._currentTemplateId) {
        await API.routeTemplates.update(this._currentTemplateId, data);
        showToast('Шаблон обновлён', 'success');
      } else {
        await API.routeTemplates.create(data);
        showToast('Шаблон сохранён', 'success');
      }
      hideModal('save-modal');
      await this.loadData();
      this.renderTemplateList();
    } catch (e) {
      showToast(e.message, 'error');
    }
  },

  // Определить порядок узлов по цепочке соединений
  getOrderedNodes() {
    if (!this.connections.length) return this.nodes;

    // Найти начальный узел (нет входящих соединений)
    const hasIncoming = new Set(this.connections.map(c => c.to));
    const startNodes = this.nodes.filter(n => !hasIncoming.has(n.id));

    if (!startNodes.length) return this.nodes;

    const ordered = [];
    const visited = new Set();

    const traverse = (nodeId) => {
      if (visited.has(nodeId)) return;
      visited.add(nodeId);
      const node = this.nodes.find(n => n.id === nodeId);
      if (node) ordered.push(node);
      const nexts = this.connections.filter(c => c.from === nodeId).map(c => c.to);
      nexts.forEach(traverse);
    };

    startNodes.forEach(n => traverse(n.id));

    // Добавить оставшиеся (не связанные)
    this.nodes.forEach(n => { if (!visited.has(n.id)) ordered.push(n); });

    return ordered;
  },

  async loadTemplate(id) {
    try {
      const template = await API.routeTemplates.getById(id);
      this.clearCanvas();
      this._currentTemplateId = id;

      // Создать узлы из шагов
      let x = 60;
      const y = 120;
      const nodeMap = {};

      template.steps
        .sort((a, b) => a.stepOrder - b.stepOrder)
        .forEach(step => {
          const node = {
            id: this.nextNodeId++,
            stepType: step.stepType,
            x, y,
            name: step.stepName,
            assignedRoleId:       step.assignedRoleId,
            assignedDepartmentId: step.assignedDepartmentId,
            assignedUserId:       step.assignedUserId,
          };
          this.nodes.push(node);
          this.renderNode(node);
          nodeMap[step.stepOrder] = node.id;
          x += 220;
        });

      // Создать соединения по порядку
      const orders = Object.keys(nodeMap).map(Number).sort((a,b) => a-b);
      for (let i = 0; i < orders.length - 1; i++) {
        this.connections.push({
          from: nodeMap[orders[i]],
          to:   nodeMap[orders[i+1]],
        });
      }

      this.redrawSVG();
      showToast(`Шаблон "${template.name}" загружен`, 'success');
    } catch (e) {
      showToast(e.message, 'error');
    }
  },

  async deleteTemplate(id) {
    confirmAction('Удалить шаблон маршрута?', async () => {
      try {
        await API.routeTemplates.delete(id);
        showToast('Шаблон удалён', 'info');
        if (this._currentTemplateId === id) {
          this.clearCanvas();
          this._currentTemplateId = null;
        }
        await this.loadData();
        this.renderTemplateList();
      } catch (e) {
        showToast(e.message, 'error');
      }
    });
  },

  clearCanvas() {
    const canvas = document.getElementById('builder-canvas');
    if (!canvas) return;
    canvas.querySelectorAll('.route-node').forEach(el => el.remove());
    this.nodes = [];
    this.connections = [];
    this.selectedNode = null;
    this.connectingFrom = null;
    this._currentTemplateId = null;
    this.redrawSVG();
    this.renderProps(null);
  },

  newTemplate() {
    this.clearCanvas();
    showToast('Новый шаблон. Перетащите шаги на рабочую область.', 'info');
  },
};
