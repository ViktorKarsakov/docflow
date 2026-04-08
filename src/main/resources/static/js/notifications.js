// ============================================================
// DOCFLOW — Уведомления
// ============================================================

const Notifications = {

  async load(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    container.innerHTML = '<div class="loading-overlay"><div class="spinner"></div></div>';

    try {
      const items = await API.notifications.getAll();

      if (!items.length) {
        container.innerHTML = `<div class="empty-state"><i class="bi bi-bell-slash"></i><p>Уведомлений нет</p></div>`;
        return;
      }

      const typeIcon = {
        'INFO':            'bi-info-circle text-blue',
        'ACTION_REQUIRED': 'bi-exclamation-circle text-warning',
        'WARNING':         'bi-exclamation-triangle text-warning',
        'URGENT':          'bi-bell-fill text-danger',
      };

      container.innerHTML = items.map(n => `
        <div class="notif-item ${n.read ? 'read' : 'unread'}" data-id="${n.id}">
          <div class="notif-icon">
            <i class="bi ${typeIcon[n.type] || 'bi-bell'}"></i>
          </div>
          <div class="notif-body">
            <div class="notif-title">${n.title}</div>
            <div class="notif-message">${n.message}</div>
            <div class="notif-time">${formatDateTime(n.createdAt)}</div>
          </div>
          <div class="notif-actions">
            ${n.link ? `<a href="${n.link}" class="btn btn-ghost btn-sm btn-icon" title="Перейти"><i class="bi bi-arrow-right"></i></a>` : ''}
            ${!n.read ? `<button class="btn btn-ghost btn-sm btn-icon" title="Прочитано" onclick="Notifications.markRead(${n.id}, this)"><i class="bi bi-check2"></i></button>` : ''}
          </div>
        </div>
      `).join('');

    } catch (e) {
      container.innerHTML = `<div class="alert alert-danger"><i class="bi bi-exclamation-circle"></i>${e.message}</div>`;
    }
  },

  async markRead(id, btn) {
    try {
      await API.notifications.markRead(id);
      const item = btn?.closest('.notif-item');
      if (item) {
        item.classList.add('read');
        item.classList.remove('unread');
        btn.remove();
      }
      await Layout.refreshCounters();
    } catch (e) {
      showToast(e.message, 'error');
    }
  },

  async markAllRead() {
    try {
      await API.notifications.markAllRead();
      document.querySelectorAll('.notif-item').forEach(el => {
        el.classList.add('read');
        el.classList.remove('unread');
        el.querySelector('[title="Прочитано"]')?.remove();
      });
      await Layout.refreshCounters();
      showToast('Все уведомления прочитаны', 'success');
    } catch (e) {
      showToast(e.message, 'error');
    }
  },
};
