// ============================================================
// DOCFLOW — Управление отделами
// ============================================================

const DepartmentsAdmin = {

  _editingId: null,

  async load() {
    const container = document.getElementById('depts-table-body');
    if (!container) return;

    container.innerHTML = '<tr><td colspan="4"><div class="loading-overlay"><div class="spinner"></div></div></td></tr>';

    try {
      const depts = await API.departments.getAll();
      container.innerHTML = depts.map(d => `
        <tr>
          <td class="fw-500">${d.name}</td>
          <td><span class="badge badge-draft">${d.code}</span></td>
          <td class="text-sec">${d.city || '—'}</td>
          <td class="actions">
            <button class="btn btn-ghost btn-sm btn-icon" onclick="DepartmentsAdmin.openEdit(${d.id}, '${d.name}', '${d.code}', '${d.city || ''}')" title="Редактировать">
              <i class="bi bi-pencil"></i>
            </button>
          </td>
        </tr>
      `).join('');
    } catch (e) {
      showToast(e.message, 'error');
    }
  },

  openCreate() {
    this._editingId = null;
    document.getElementById('dept-modal-title').textContent = 'Новый отдел';
    document.getElementById('dept-form').reset();
    showModal('dept-modal');
  },

  openEdit(id, name, code, city) {
    this._editingId = id;
    document.getElementById('dept-modal-title').textContent = 'Редактировать отдел';
    document.getElementById('dept-name').value = name;
    document.getElementById('dept-code').value = code;
    document.getElementById('dept-city').value = city;
    showModal('dept-modal');
  },

  async save() {
    const data = {
      name: document.getElementById('dept-name').value,
      code: document.getElementById('dept-code').value,
      city: document.getElementById('dept-city').value || null,
    };

    try {
      if (this._editingId) {
        await API.put(`/departments/${this._editingId}`, data);
        showToast('Отдел обновлён', 'success');
      } else {
        await API.post('/departments', data);
        showToast('Отдел создан', 'success');
      }
      hideModal('dept-modal');
      await this.load();
    } catch (e) {
      showToast(e.message, 'error');
    }
  },
};
