// ============================================================
// DOCFLOW — Управление ролями (администратор)
// ============================================================

const RolesAdmin = {

  // Системные роли — показываем как защищённые, кнопку удаления не показываем
  PROTECTED: ['ROLE_ADMIN', 'ROLE_CHIEF', 'ROLE_EMPLOYEE'],

  async load() {
    const container = document.getElementById('roles-table-body');
    if (!container) return;

    container.innerHTML = '<tr><td colspan="4"><div class="loading-overlay"><div class="spinner"></div></div></td></tr>';

    try {
      const roles = await API.roles.getAll();

      if (!roles.length) {
        container.innerHTML = '<tr><td colspan="4"><div class="empty-state"><i class="bi bi-shield-x"></i><p>Ролей нет</p></div></td></tr>';
        return;
      }

      container.innerHTML = roles.map(r => {
        const isProtected = this.PROTECTED.includes(r.name);
        return `
          <tr>
            <td class="fw-500">${r.displayName}</td>
            <td><code style="font-size:0.8rem;background:var(--surface-alt);padding:0.2rem 0.4rem;border-radius:4px;border:1px solid var(--border)">${r.name}</code></td>
            <td>
              ${isProtected
                ? '<span class="badge badge-approval"><i class="bi bi-shield-fill"></i> Системная</span>'
                : '<span class="badge badge-draft">Пользовательская</span>'
              }
            </td>
            <td class="actions">
              ${!isProtected
                ? `<button class="btn btn-ghost btn-sm btn-icon text-danger"
                           onclick="RolesAdmin.delete(${r.id}, '${r.displayName}')"
                           title="Удалить">
                     <i class="bi bi-trash"></i>
                   </button>`
                : '<span style="width:32px;display:inline-block"></span>'
              }
            </td>
          </tr>
        `;
      }).join('');

    } catch (e) {
      container.innerHTML = `<tr><td colspan="4"><div class="alert alert-danger"><i class="bi bi-exclamation-circle"></i>${e.message}</div></td></tr>`;
    }
  },

  openCreate() {
    document.getElementById('role-form').reset();
    document.getElementById('role-name-preview').textContent = 'ROLE_...';
    showModal('role-modal');
    // Фокус на первое поле
    setTimeout(() => document.getElementById('role-displayName').focus(), 100);
  },

  async save() {
    const displayName = document.getElementById('role-displayName').value?.trim();
    const name = document.getElementById('role-name').value?.trim();

    if (!displayName) { showToast('Введите название роли', 'error'); return; }
    if (!name)        { showToast('Введите системное имя роли', 'error'); return; }

    try {
      await API.post('/roles', { name, displayName });
      hideModal('role-modal');
      showToast('Роль создана', 'success');
      await this.load();
    } catch (e) {
      showToast(e.message, 'error');
    }
  },

  async delete(id, displayName) {
    confirmAction(
      `Удалить роль «${displayName}»?\n\nЕсли роль назначена пользователям или используется в маршрутах — удаление будет отклонено.`,
      async () => {
        try {
          await API.delete(`/roles/${id}`);
          showToast('Роль удалена', 'info');
          await this.load();
        } catch (e) {
          showToast(e.message, 'error');
        }
      }
    );
  },
};
