// ============================================================
// DOCFLOW — Управление пользователями (администратор)
// ============================================================

const UsersAdmin = {

  _allDepts: [],
  _allRoles: [],
  _editingId: null,

  async init() {
    const [depts, roles] = await Promise.all([
      API.departments.getAll().catch(() => []),
      API.roles.getAll().catch(() => []),
    ]);
    this._allDepts = depts;
    this._allRoles = roles;
    this.populateFormSelects();
    await this.load();
  },

  populateFormSelects() {
    const deptSelect = document.getElementById('form-dept');
    if (deptSelect) {
      deptSelect.innerHTML = '<option value="">— Не выбран —</option>' +
        this._allDepts.map(d => `<option value="${d.id}">${d.name}</option>`).join('');
    }

    const rolesContainer = document.getElementById('form-roles');
    if (rolesContainer) {
      rolesContainer.innerHTML = this._allRoles.map(r => `
        <label class="form-check">
          <input type="checkbox" name="roles" value="${r.name}">
          ${r.displayName}
        </label>
      `).join('');
    }
  },

  async load() {
    const container = document.getElementById('users-table-body');
    if (!container) return;

    container.innerHTML = '<tr><td colspan="6"><div class="loading-overlay"><div class="spinner"></div></div></td></tr>';

    try {
      const users = await API.users.getAll();
      container.innerHTML = users.map(u => `
        <tr class="${!u.active ? 'user-inactive' : ''}">
          <td>
            <div class="user-avatar-cell">
              <div class="user-avatar-sm">${Auth.getInitials(u.fullName)}</div>
              <div>
                <div class="fw-500">${u.fullName}</div>
                <div class="text-muted" style="font-size:0.75rem">${u.username}</div>
              </div>
            </div>
          </td>
          <td class="text-sec">${u.position}</td>
          <td class="text-sec">${u.departmentName || '—'}</td>
          <td>${(u.roles || []).map(r => `<span class="badge badge-draft">${Auth.getRoleDisplayName(r)}</span>`).join(' ')}</td>
          <td>${u.active ? '<span class="badge badge-approved">Активен</span>' : '<span class="badge badge-withdrawn">Неактивен</span>'}</td>
          <td class="actions">
            <button class="btn btn-ghost btn-sm btn-icon" onclick="UsersAdmin.openEdit(${u.id})" title="Редактировать"><i class="bi bi-pencil"></i></button>
            ${u.active ? `<button class="btn btn-ghost btn-sm btn-icon text-danger" onclick="UsersAdmin.deactivate(${u.id})" title="Деактивировать"><i class="bi bi-person-x"></i></button>` : ''}
          </td>
        </tr>
      `).join('');
    } catch (e) {
      showToast(e.message, 'error');
    }
  },

  openCreate() {
    this._editingId = null;
    document.getElementById('modal-title').textContent = 'Новый пользователь';
    document.getElementById('user-form').reset();
    document.getElementById('field-password').querySelector('input').required = true;
    showModal('user-modal');
  },

  async openEdit(id) {
    try {
      const user = await API.users.getById(id);
      this._editingId = id;
      document.getElementById('modal-title').textContent = 'Редактировать пользователя';

      const form = document.getElementById('user-form');
      form.querySelector('[name="username"]').value = user.username;
      form.querySelector('[name="fullName"]').value = user.fullName;
      form.querySelector('[name="position"]').value = user.position;
      form.querySelector('[name="email"]').value = user.email || '';
      form.querySelector('[name="phone"]').value = user.phone || '';

      const deptSelect = form.querySelector('[name="departmentId"]');
      if (deptSelect && user.departmentId) deptSelect.value = user.departmentId;

      // Роли
      form.querySelectorAll('[name="roles"]').forEach(cb => {
        cb.checked = user.roles?.includes(cb.value);
      });

      // Пароль необязателен при редактировании
      document.getElementById('field-password').querySelector('input').required = false;

      showModal('user-modal');
    } catch (e) {
      showToast(e.message, 'error');
    }
  },

  async save() {
    const form = document.getElementById('user-form');
    const fd = new FormData(form);

    const roles = [...form.querySelectorAll('[name="roles"]:checked')].map(el => el.value);

    const data = {
      username:     fd.get('username'),
      password:     fd.get('password') || undefined,
      fullName:     fd.get('fullName'),
      position:     fd.get('position'),
      email:        fd.get('email') || undefined,
      phone:        fd.get('phone') || undefined,
      departmentId: fd.get('departmentId') ? Number(fd.get('departmentId')) : undefined,
      roles,
    };

    // Убрать undefined
    Object.keys(data).forEach(k => data[k] === undefined && delete data[k]);

    try {
      if (this._editingId) {
        await API.users.update(this._editingId, data);
        showToast('Пользователь обновлён', 'success');
      } else {
        await API.users.create(data);
        showToast('Пользователь создан', 'success');
      }
      hideModal('user-modal');
      await this.load();
    } catch (e) {
      showToast(e.message, 'error');
    }
  },

  async deactivate(id) {
    confirmAction('Деактивировать пользователя? Его документы сохранятся.', async () => {
      try {
        await API.users.deactivate(id);
        showToast('Пользователь деактивирован', 'info');
        await this.load();
      } catch (e) {
        showToast(e.message, 'error');
      }
    });
  },
};
