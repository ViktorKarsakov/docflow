// ============================================================
// DOCFLOW — Редактор маршрута согласования (список шагов)
// ============================================================

const RouteEditor = {

  _typeId: null,
  _typeName: '',
  _templateId: null,   // ID существующего шаблона (null если маршрута ещё нет)
  _steps: [],          // Текущий список шагов
  _editingIndex: null, // Индекс редактируемого шага (null = добавление)

  _roles: [],
  _departments: [],
  _users: [],

  // ============================================================
  // ИНИЦИАЛИЗАЦИЯ
  // ============================================================
  async init(typeId) {
    this._typeId = typeId;

    try {
      // Загружаем всё параллельно
      const [docType, roles, depts, users, templates] = await Promise.all([
        API.get(`/document-types/${typeId}`),
        API.roles.getAll().catch(() => []),
        API.departments.getAll().catch(() => []),
        API.users.getAll().catch(() => []),
        API.routeTemplates.getAll().catch(() => []),
      ]);

      this._typeName = docType.displayName;
      this._roles = roles;
      this._departments = depts;
      this._users = users;

      // Ищем существующий маршрут для этого типа
      const existing = templates.find(t => t.documentTypeId === typeId);
      if (existing) {
        this._templateId = existing.id;
        // Загружаем шаги с полными данными
        const full = await API.routeTemplates.getById(existing.id);
        this._steps = full.steps.sort((a, b) => a.stepOrder - b.stepOrder).map(s => ({
          stepName:             s.stepName,
          stepType:             s.stepType,
          assignedRoleId:       s.assignedRoleId,
          assignedDepartmentId: s.assignedDepartmentId,
          assignedUserId:       s.assignedUserId,
          // Для отображения
          assignedRoleDisplayName: s.assignedRoleDisplayName,
          assignedDepartmentName:  s.assignedDepartmentName,
          assignedUserFullName:     s.assignedUserFullName,
        }));
      } else {
        this._steps = [];
      }

      // Обновляем заголовки страницы
      document.getElementById('bc-doctype').textContent = this._typeName;
      document.getElementById('page-title').textContent = `Маршрут: ${this._typeName}`;
      document.getElementById('page-subtitle').textContent = existing
        ? `Шаблон настроен — ${this._steps.length} шаг(ов)`
        : 'Маршрут ещё не настроен — добавьте шаги';

      // Заполняем выпадающие списки
      this.populateSelects();

      // Рендерим шаги
      this.renderSteps();

    } catch (e) {
      showToast('Ошибка загрузки: ' + e.message, 'error');
    }
  },

  // Заполнить выпадающие списки ролей, отделов, сотрудников
  populateSelects() {
    const roleSelect = document.getElementById('step-role');
    roleSelect.innerHTML = '<option value="">— Выберите роль —</option>' +
      this._roles.map(r => `<option value="${r.id}">${r.displayName}</option>`).join('');

    const deptSelect = document.getElementById('step-dept');
    deptSelect.innerHTML = '<option value="">— Выберите отдел —</option>' +
      this._departments.map(d => `<option value="${d.id}">${d.name}</option>`).join('');

    const userSelect = document.getElementById('step-user');
    userSelect.innerHTML = '<option value="">— Выберите сотрудника —</option>' +
      this._users.map(u => `<option value="${u.id}">${u.fullName}${u.departmentName ? ' — ' + u.departmentName : ''}</option>`).join('');
  },

  // ============================================================
  // РЕНДЕР СПИСКА ШАГОВ
  // ============================================================
  renderSteps() {
    const container = document.getElementById('steps-list');
    const counter = document.getElementById('steps-count');

    counter.textContent = this._steps.length
      ? `${this._steps.length} шаг(ов)`
      : '';

    if (!this._steps.length) {
      container.innerHTML = `
        <div class="empty-state">
          <i class="bi bi-diagram-3"></i>
          <p>Шагов пока нет. Добавьте первый шаг с помощью формы справа.</p>
        </div>
      `;
      return;
    }

    const stepTypeLabel = {
      APPROVAL:   'Согласование',
      SIGNATURE:  'Подпись',
      EXECUTION:  'Исполнение',
      RESOLUTION: 'Резолюция',
    };

    container.innerHTML = this._steps.map((s, i) => {
      const assignee = s.assignedRoleDisplayName
        || s.assignedDepartmentName
        || s.assignedUserFullName
        || '—';

      const isFirst = i === 0;
      const isLast  = i === this._steps.length - 1;

      return `
        ${i > 0 ? '<div class="step-arrow"><i class="bi bi-arrow-down"></i></div>' : ''}
        <div class="step-item" id="step-item-${i}">
          <div class="step-number">${i + 1}</div>
          <div class="step-info">
            <div class="step-name">${s.stepName}</div>
            <div class="step-meta">${stepTypeLabel[s.stepType] || s.stepType} · ${assignee}</div>
          </div>
          <span class="step-type-badge step-type-${s.stepType}">${stepTypeLabel[s.stepType]}</span>
          <div class="step-actions">
            <button class="btn btn-ghost btn-sm btn-icon" onclick="RouteEditor.moveUp(${i})"
                    title="Выше" ${isFirst ? 'disabled' : ''}>
              <i class="bi bi-arrow-up"></i>
            </button>
            <button class="btn btn-ghost btn-sm btn-icon" onclick="RouteEditor.moveDown(${i})"
                    title="Ниже" ${isLast ? 'disabled' : ''}>
              <i class="bi bi-arrow-down"></i>
            </button>
            <button class="btn btn-ghost btn-sm btn-icon" onclick="RouteEditor.editStep(${i})"
                    title="Редактировать">
              <i class="bi bi-pencil"></i>
            </button>
            <button class="btn btn-ghost btn-sm btn-icon text-danger" onclick="RouteEditor.removeStep(${i})"
                    title="Удалить">
              <i class="bi bi-trash"></i>
            </button>
          </div>
        </div>
      `;
    }).join('');
  },

  // ============================================================
  // ФОРМА ДОБАВЛЕНИЯ / РЕДАКТИРОВАНИЯ
  // ============================================================

  // Переключить видимость полей при смене типа назначения
  onAssignTypeChange() {
    const type = document.getElementById('assign-type').value;
    document.getElementById('field-role').style.display = type === 'role'       ? '' : 'none';
    document.getElementById('field-dept').style.display = type === 'department' ? '' : 'none';
    document.getElementById('field-user').style.display = type === 'user'       ? '' : 'none';
  },

  // Добавить или сохранить редактируемый шаг
  addStep() {
    const name = document.getElementById('step-name').value.trim();
    const type = document.getElementById('step-type').value;
    const assignType = document.getElementById('assign-type').value;

    if (!name) { showToast('Введите название шага', 'error'); return; }

    let assignedRoleId = null, assignedDepartmentId = null, assignedUserId = null;
    let assignedRoleDisplayName = null, assignedDepartmentName = null, assignedUserFullName = null;

    if (assignType === 'role') {
      assignedRoleId = Number(document.getElementById('step-role').value) || null;
      if (!assignedRoleId) { showToast('Выберите роль', 'error'); return; }
      const role = this._roles.find(r => r.id === assignedRoleId);
      assignedRoleDisplayName = role?.displayName;
    } else if (assignType === 'department') {
      assignedDepartmentId = Number(document.getElementById('step-dept').value) || null;
      if (!assignedDepartmentId) { showToast('Выберите отдел', 'error'); return; }
      const dept = this._departments.find(d => d.id === assignedDepartmentId);
      assignedDepartmentName = dept?.name;
    } else {
      assignedUserId = Number(document.getElementById('step-user').value) || null;
      if (!assignedUserId) { showToast('Выберите сотрудника', 'error'); return; }
      const user = this._users.find(u => u.id === assignedUserId);
      assignedUserFullName = user?.fullName;
    }

    const step = {
      stepName: name,
      stepType: type,
      assignedRoleId, assignedDepartmentId, assignedUserId,
      assignedRoleDisplayName, assignedDepartmentName, assignedUserFullName,
    };

    if (this._editingIndex !== null) {
      // Редактирование существующего шага
      this._steps[this._editingIndex] = step;
      this.cancelEdit();
    } else {
      // Добавление нового
      this._steps.push(step);
      this.resetForm();
    }

    this.renderSteps();
    showToast(this._editingIndex !== null ? 'Шаг обновлён' : 'Шаг добавлен', 'success');
  },

  // Открыть шаг для редактирования
  editStep(index) {
    const s = this._steps[index];
    this._editingIndex = index;

    document.getElementById('step-form-title').textContent = 'Редактировать шаг';
    document.getElementById('add-btn-label').textContent = 'Сохранить изменения';
    document.getElementById('cancel-edit-btn').style.display = '';
    document.getElementById('step-name').value = s.stepName;
    document.getElementById('step-type').value = s.stepType;

    if (s.assignedUserId) {
      document.getElementById('assign-type').value = 'user';
      document.getElementById('step-user').value = s.assignedUserId;
    } else if (s.assignedDepartmentId) {
      document.getElementById('assign-type').value = 'department';
      document.getElementById('step-dept').value = s.assignedDepartmentId;
    } else {
      document.getElementById('assign-type').value = 'role';
      document.getElementById('step-role').value = s.assignedRoleId || '';
    }

    this.onAssignTypeChange();
    document.getElementById('step-name').focus();

    // Подсветить редактируемый шаг
    document.querySelectorAll('.step-item').forEach(el => el.style.background = '');
    const item = document.getElementById(`step-item-${index}`);
    if (item) item.style.background = 'var(--blue-pale)';
  },

  cancelEdit() {
    this._editingIndex = null;
    document.getElementById('step-form-title').textContent = 'Новый шаг';
    document.getElementById('add-btn-label').textContent = 'Добавить шаг';
    document.getElementById('cancel-edit-btn').style.display = 'none';
    document.querySelectorAll('.step-item').forEach(el => el.style.background = '');
    this.resetForm();
  },

  resetForm() {
    document.getElementById('step-name').value = '';
    document.getElementById('step-type').value = 'APPROVAL';
    document.getElementById('assign-type').value = 'role';
    document.getElementById('step-role').value = '';
    document.getElementById('step-dept').value = '';
    document.getElementById('step-user').value = '';
    this.onAssignTypeChange();
  },

  // ============================================================
  // УПРАВЛЕНИЕ ПОРЯДКОМ И УДАЛЕНИЕ
  // ============================================================
  removeStep(index) {
    confirmAction(`Удалить шаг «${this._steps[index].stepName}»?`, () => {
      this._steps.splice(index, 1);
      if (this._editingIndex === index) this.cancelEdit();
      this.renderSteps();
    });
  },

  moveUp(index) {
    if (index === 0) return;
    [this._steps[index - 1], this._steps[index]] = [this._steps[index], this._steps[index - 1]];
    this.renderSteps();
  },

  moveDown(index) {
    if (index === this._steps.length - 1) return;
    [this._steps[index], this._steps[index + 1]] = [this._steps[index + 1], this._steps[index]];
    this.renderSteps();
  },

  // ============================================================
  // СОХРАНЕНИЕ
  // ============================================================
  async save() {
    if (!this._steps.length) {
      showToast('Добавьте хотя бы один шаг', 'error');
      return;
    }

    const data = {
      name:           `Маршрут: ${this._typeName}`,
      documentTypeId: this._typeId,
      active:         true,
      steps:          this._steps.map((s, i) => ({
        stepOrder:            i + 1,
        stepName:             s.stepName,
        stepType:             s.stepType,
        assignedRoleId:       s.assignedRoleId,
        assignedDepartmentId: s.assignedDepartmentId,
        assignedUserId:       s.assignedUserId,
      })),
    };

    try {
      if (this._templateId) {
        await API.routeTemplates.update(this._templateId, data);
      } else {
        const created = await API.routeTemplates.create(data);
        this._templateId = created.id;
      }
      showToast('Маршрут сохранён', 'success');

      // Обновляем подзаголовок
      document.getElementById('page-subtitle').textContent =
        `Шаблон настроен — ${this._steps.length} шаг(ов)`;
    } catch (e) {
      showToast(e.message, 'error');
    }
  },
};
