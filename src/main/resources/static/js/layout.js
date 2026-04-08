// ============================================================
// DOCFLOW — Лейаут: сайдбар, шапка, уведомления
// ============================================================

const Layout = {

  // Инициализация страницы (вызывается на каждой странице)
  async init(pageTitle) {
    const user = await Auth.getUser();
    if (!user) return;

    this.renderSidebar(user, pageTitle);
    this.renderTopbar(user, pageTitle);
    this.initNotifBell(user);

    // Тост-контейнер
    if (!document.getElementById('toast-container')) {
      const tc = document.createElement('div');
      tc.id = 'toast-container';
      tc.className = 'toast-container';
      document.body.appendChild(tc);
    }

    return user;
  },

  // Рендер сайдбара
  renderSidebar(user, activeTitle) {
    const isAdmin = Auth.hasRole(user, 'ROLE_ADMIN');
    const isChief = Auth.hasRole(user, 'ROLE_CHIEF');

    const nav = [
      { icon: 'bi-speedometer2', label: 'Дашборд',         href: '/pages/dashboard.html',     title: 'Дашборд' },
      { icon: 'bi-file-text',    label: 'Мои документы',   href: '/pages/documents-my.html',  title: 'Мои документы' },
      { icon: 'bi-inbox',        label: 'Входящие',         href: '/pages/documents-inbox.html', title: 'Входящие', badge: 'inbox' },
      { icon: 'bi-check2-square',label: 'Поручения',        href: '/pages/tasks.html',         title: 'Поручения' },
      { icon: 'bi-bell',         label: 'Уведомления',      href: '/pages/notifications.html', title: 'Уведомления', badge: 'notif' },
    ];

    let adminNav = [];
    if (isAdmin) {
      adminNav = [
        { icon: 'bi-people',      label: 'Пользователи',    href: '/pages/admin/users.html',        title: 'Пользователи' },
        { icon: 'bi-shield-check', label: 'Роли', href: '/pages/admin/roles.html', title: 'Роли' },
        { icon: 'bi-diagram-3',   label: 'Маршруты',         href: '/pages/admin/routes.html',       title: 'Маршруты' },
        { icon: 'bi-building',    label: 'Отделы',           href: '/pages/admin/departments.html',  title: 'Отделы' },
        { icon: 'bi-file-earmark-text', label: 'Типы документов', href: '/pages/admin/doc-types.html', title: 'Типы документов' },
      ];
    }

    const navItems = (items) => items.map(item => {
      const active = item.title === activeTitle ? 'active' : '';
      const badge = item.badge ? `<span class="sidebar-badge" id="badge-${item.badge}" style="display:none">0</span>` : '';
      return `<a class="sidebar-item ${active}" href="${item.href}">
        <i class="bi ${item.icon}"></i>${item.label}${badge}
      </a>`;
    }).join('');

    const adminSection = adminNav.length ? `
      <div class="sidebar-section">
        <div class="sidebar-label">Администрирование</div>
        ${navItems(adminNav)}
      </div>
    ` : '';

    const initials = Auth.getInitials(user.fullName);
    const roleDisplay = user.roles?.length
      ? Auth.getRoleDisplayName(user.roles[0])
      : '';

    const html = `
      <div class="sidebar-logo">
        <div class="sidebar-logo-icon"><i class="bi bi-file-earmark-check"></i></div>
        <div class="sidebar-logo-text">
          ЭДО <small>КГБУЗ КККВД №1</small>
        </div>
      </div>
      <div class="sidebar-section">
        <div class="sidebar-label">Меню</div>
        ${navItems(nav)}
      </div>
      ${adminSection}
      <div class="sidebar-user">
        <div class="sidebar-avatar">${initials}</div>
        <div class="sidebar-user-info">
          <div class="sidebar-user-name">${user.fullName}</div>
          <div class="sidebar-user-role">${roleDisplay}</div>
        </div>
        <button class="sidebar-logout" onclick="Auth.logout()" title="Выйти">
          <i class="bi bi-box-arrow-right"></i>
        </button>
      </div>
    `;

    const sidebar = document.getElementById('sidebar');
    if (sidebar) sidebar.innerHTML = html;
  },

  // Рендер шапки
  renderTopbar(user, pageTitle) {
    const topbarTitle = document.getElementById('topbar-title');
    if (topbarTitle) topbarTitle.textContent = pageTitle;
  },

  // Инициализация счётчика уведомлений
  async initNotifBell(user) {
    try {
      const data = await API.notifications.getUnreadCount();
      const count = data?.count || 0;

      // Обновить бейдж уведомлений в сайдбаре
      const notifBadge = document.getElementById('badge-notif');
      if (notifBadge) {
        notifBadge.textContent = count;
        notifBadge.style.display = count > 0 ? '' : 'none';
      }

      // Обновить точку на колокольчике в шапке
      const dot = document.getElementById('notif-dot');
      if (dot) {
        dot.style.display = count > 0 ? '' : 'none';
      }

      // Обновить входящие
      const inboxData = await API.documents.getInbox();
      const inboxCount = inboxData?.length || 0;
      const inboxBadge = document.getElementById('badge-inbox');
      if (inboxBadge) {
        inboxBadge.textContent = inboxCount;
        inboxBadge.style.display = inboxCount > 0 ? '' : 'none';
      }
    } catch (e) {
      // Тихо игнорируем — не критично
    }
  },

  // Обновить счётчики (вызывается после действий)
  async refreshCounters() {
    const user = await Auth.getUser();
    if (user) await this.initNotifBell(user);
  }
};

// ============================================================
// Утилиты для UI
// ============================================================

// Показать toast-уведомление
function showToast(message, type = 'info') {
  const icons = { success: 'bi-check-circle-fill', error: 'bi-x-circle-fill', info: 'bi-info-circle-fill' };
  const tc = document.getElementById('toast-container');
  if (!tc) return;

  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `<i class="bi ${icons[type] || icons.info}"></i><span>${message}</span>`;
  tc.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transition = 'opacity 0.3s';
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

// Показать модальное окно
function showModal(id) {
  document.getElementById(id)?.classList.remove('hidden');
}

// Скрыть модальное окно
function hideModal(id) {
  document.getElementById(id)?.classList.add('hidden');
}

// Форматировать дату
function formatDate(dateStr) {
  if (!dateStr) return '—';
  const d = new Date(dateStr);
  return d.toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

// Форматировать дату+время
function formatDateTime(dateStr) {
  if (!dateStr) return '—';
  const d = new Date(dateStr);
  return d.toLocaleDateString('ru-RU', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  });
}

// Бейдж статуса документа
function docStatusBadge(status) {
  const map = {
    'DRAFT':       ['badge-draft',     'Черновик'],
    'ON_APPROVAL': ['badge-approval',  'На согласовании'],
    'APPROVED':    ['badge-approved',  'Согласован'],
    'ON_EXECUTION':['badge-execution', 'На исполнении'],
    'COMPLETED':   ['badge-completed', 'Завершён'],
    'REJECTED':    ['badge-rejected',  'Отклонён'],
    'WITHDRAWN':   ['badge-withdrawn', 'Отозван'],
  };
  const [cls, label] = map[status] || ['badge-draft', status];
  return `<span class="badge ${cls}">${label}</span>`;
}

// Бейдж статуса поручения
function taskStatusBadge(status) {
  const map = {
    'NEW':        ['badge-new',        'Новое'],
    'IN_PROGRESS':['badge-inprogress', 'В работе'],
    'COMPLETED':  ['badge-completed2', 'Выполнено'],
    'CANCELLED':  ['badge-cancelled',  'Отменено'],
  };
  const [cls, label] = map[status] || ['badge-draft', status];
  return `<span class="badge ${cls}">${label}</span>`;
}

// Подтверждение действия
function confirmAction(message, callback) {
  if (confirm(message)) callback();
}

// Установить активный таб
function initTabs(containerSelector) {
  const container = document.querySelector(containerSelector);
  if (!container) return;

  container.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const target = btn.dataset.tab;
      container.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
      container.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
      btn.classList.add('active');
      document.getElementById(target)?.classList.add('active');
    });
  });
}

// Простой debounce для поиска
function debounce(fn, delay) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), delay);
  };
}
