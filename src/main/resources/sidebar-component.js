/**
 * 侧边栏菜单组件 - 抽象模块
 * 支持动态配置、主题切换、功能扩展
 */

class SidebarComponent {
    constructor(containerSelector, options = {}) {
        this.container = document.querySelector(containerSelector);
        this.options = {
            header: options.header || '工具集',
            theme: options.theme || 'default',
            onNavigate: options.onNavigate || this._defaultNavigate.bind(this),
            onReady: options.onReady || null,
            ...options
        };
        
        this.menus = [];
        this.activeItem = null;
        this.themes = {
            default: {
                primaryColor: '#667eea',
                bgColor: 'white',
                textColor: '#333',
                activeBg: '#f0f4ff',
                hoverBg: '#f5f5f5'
            },
            dark: {
                primaryColor: '#4a69bd',
                bgColor: '#2d3436',
                textColor: '#dfe6e9',
                activeBg: '#3d4852',
                hoverBg: '#636e72'
            },
            light: {
                primaryColor: '#0984e3',
                bgColor: '#ffffff',
                textColor: '#2d3436',
                activeBg: '#e3f2fd',
                hoverBg: '#f5f5f5'
            }
        };
        
        this._init();
    }
    
    _init() {
        this._injectStyles();
        this._render();
        this._bindEvents();
        
        if (this.options.onReady) {
            this.options.onReady(this);
        }
    }
    
    _injectStyles() {
        if (document.getElementById('sidebar-component-styles')) return;
        
        const style = document.createElement('style');
        style.id = 'sidebar-component-styles';
        style.textContent = `
            .sidebar-component {
                width: 140px;
                flex-shrink: 0;
                display: flex;
                flex-direction: column;
                gap: 8px;
                font-family: "Public Sans", -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            }
            
            .sidebar-component-item {
                background: var(--sb-bg-color, white);
                border-radius: 8px;
                overflow: hidden;
                box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
            }
            
            .sidebar-component-header {
                background: var(--sb-primary-color, #667eea);
                color: white;
                padding: 10px 12px;
                font-size: 12px;
                font-weight: 500;
                text-align: center;
            }
            
            .sidebar-component-nav {
                display: flex;
                flex-direction: column;
            }
            
            .sidebar-component-link {
                display: flex;
                align-items: center;
                gap: 8px;
                padding: 10px 12px;
                font-size: 13px;
                color: var(--sb-text-color, #333);
                text-decoration: none;
                transition: all 0.2s ease;
                cursor: pointer;
                border-left: 3px solid transparent;
                user-select: none;
            }
            
            .sidebar-component-link:hover {
                background: var(--sb-hover-bg, #f5f5f5);
                color: var(--sb-primary-color, #667eea);
            }
            
            .sidebar-component-link.active {
                background: var(--sb-active-bg, #f0f4ff);
                border-left-color: var(--sb-primary-color, #667eea);
                color: var(--sb-primary-color, #667eea);
                font-weight: 500;
            }
            
            .sidebar-component-link .icon {
                font-size: 16px;
                flex-shrink: 0;
            }
            
            .sidebar-component-link .link-text {
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }
            
            .sidebar-component-link.disabled {
                opacity: 0.5;
                pointer-events: none;
            }
            
            .sidebar-component-divider {
                height: 1px;
                background: rgba(128, 128, 128, 0.2);
                margin: 4px 12px;
            }
            
            .sidebar-component-badge {
                font-size: 10px;
                background: var(--sb-primary-color, #667eea);
                color: white;
                padding: 2px 6px;
                border-radius: 10px;
                margin-left: auto;
            }
            
            @media (max-width: 768px) {
                .sidebar-component {
                    width: 100%;
                    flex-direction: row;
                    padding: 8px;
                }
                
                .sidebar-component-item {
                    flex: 1;
                }
                
                .sidebar-component-header {
                    display: none;
                }
                
                .sidebar-component-nav {
                    flex-direction: row;
                }
                
                .sidebar-component-link {
                    flex: 1;
                    justify-content: center;
                    padding: 10px;
                    font-size: 12px;
                    border-left: none;
                    border-bottom: 2px solid transparent;
                }
                
                .sidebar-component-link.active {
                    border-left: none;
                    border-bottom-color: var(--sb-primary-color, #667eea);
                }
                
                .sidebar-component-link .link-text {
                    display: none;
                }
                
                .sidebar-component-badge {
                    display: none;
                }
            }
        `;
        document.head.appendChild(style);
    }
    
    _render() {
        const theme = this.themes[this.options.theme] || this.themes.default;
        
        this.container.innerHTML = `
            <div class="sidebar-component-item" data-theme="${this.options.theme}">
                ${this.options.header ? `<div class="sidebar-component-header">${this._escapeHtml(this.options.header)}</div>` : ''}
                <nav class="sidebar-component-nav">
                    ${this.menus.map((menu, index) => this._renderMenuItem(menu, index)).join('')}
                </nav>
            </div>
        `;
        
        this._applyTheme(theme);
    }
    
    _renderMenuItem(menu, index) {
        const isActive = menu.active || this._isCurrentPage(menu.page);
        const disabled = menu.disabled ? 'disabled' : '';
        const badge = menu.badge ? `<span class="sidebar-component-badge">${this._escapeHtml(menu.badge)}</span>` : '';
        
        if (menu.type === 'divider') {
            return '<div class="sidebar-component-divider"></div>';
        }
        
        return `
            <a class="sidebar-component-link ${isActive ? 'active' : ''} ${disabled}" 
               data-page="${this._escapeHtml(menu.page || '')}" 
               data-index="${index}"
               ${menu.target ? `target="${menu.target}"` : ''}>
                <span class="icon">${menu.icon || '📄'}</span>
                <span class="link-text">${this._escapeHtml(menu.label)}</span>
                ${badge}
            </a>
        `;
    }
    
    _bindEvents() {
        this.container.addEventListener('click', (e) => {
            const link = e.target.closest('.sidebar-component-link');
            if (link && !link.classList.contains('disabled')) {
                const index = parseInt(link.dataset.index);
                const menu = this.menus[index];
                
                if (menu && menu.onClick) {
                    menu.onClick(menu, this);
                } else {
                    this.options.onNavigate(menu.page, menu.target);
                }
            }
        });
    }
    
    _defaultNavigate(page, target) {
        if (!page) return;
        
        if (target === '_blank') {
            window.open(page, '_blank');
        } else {
            window.location.href = page;
        }
    }
    
    _isCurrentPage(page) {
        if (!page) return false;
        const currentPage = window.location.pathname.split('/').pop() || 'index.html';
        return currentPage === page;
    }
    
    _escapeHtml(str) {
        if (str === null || str === undefined) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }
    
    _applyTheme(theme) {
        const item = this.container.querySelector('.sidebar-component-item');
        if (item) {
            item.style.setProperty('--sb-primary-color', theme.primaryColor);
            item.style.setProperty('--sb-bg-color', theme.bgColor);
            item.style.setProperty('--sb-text-color', theme.textColor);
            item.style.setProperty('--sb-active-bg', theme.activeBg);
            item.style.setProperty('--sb-hover-bg', theme.hoverBg);
        }
    }
    
    // ============ 公共 API ============
    
    /**
     * 设置菜单数据
     * @param {Array} menus - 菜单配置数组
     */
    setMenus(menus) {
        this.menus = menus;
        this._render();
    }
    
    /**
     * 获取菜单数据
     * @returns {Array} 当前菜单配置
     */
    getMenus() {
        return [...this.menus];
    }
    
    /**
     * 添加菜单项
     * @param {Object} menu - 菜单配置对象
     * @param {number} index - 插入位置（可选，默认末尾）
     */
    addMenu(menu, index = -1) {
        if (index < 0 || index >= this.menus.length) {
            this.menus.push(menu);
        } else {
            this.menus.splice(index, 0, menu);
        }
        this._render();
    }
    
    /**
     * 移除菜单项
     * @param {number|string} identifier - 索引或 page 标识
     */
    removeMenu(identifier) {
        if (typeof identifier === 'number') {
            this.menus.splice(identifier, 1);
        } else {
            const index = this.menus.findIndex(m => m.page === identifier);
            if (index > -1) {
                this.menus.splice(index, 1);
            }
        }
        this._render();
    }
    
    /**
     * 更新菜单项
     * @param {number|string} identifier - 索引或 page 标识
     * @param {Object} updates - 更新的属性
     */
    updateMenu(identifier, updates) {
        let menu;
        
        if (typeof identifier === 'number') {
            menu = this.menus[identifier];
        } else {
            menu = this.menus.find(m => m.page === identifier);
        }
        
        if (menu) {
            Object.assign(menu, updates);
            this._render();
        }
    }
    
    /**
     * 设置主题
     * @param {string} themeName - 主题名称 (default|dark|light)
     */
    setTheme(themeName) {
        if (this.themes[themeName]) {
            this.options.theme = themeName;
            this._applyTheme(this.themes[themeName]);
            this.container.querySelector('.sidebar-component-item')?.setAttribute('data-theme', themeName);
        }
    }
    
    /**
     * 获取可用主题列表
     * @returns {string[]} 主题名称数组
     */
    getAvailableThemes() {
        return Object.keys(this.themes);
    }
    
    /**
     * 添加自定义主题
     * @param {string} name - 主题名称
     * @param {Object} themeConfig - 主题配置
     */
    addTheme(name, themeConfig) {
        this.themes[name] = themeConfig;
    }
    
    /**
     * 设置活动菜单项
     * @param {number|string} identifier - 索引或 page 标识
     */
    setActive(identifier) {
        this.menus.forEach(m => m.active = false);
        
        let menu;
        if (typeof identifier === 'number') {
            menu = this.menus[identifier];
        } else {
            menu = this.menus.find(m => m.page === identifier);
        }
        
        if (menu) {
            menu.active = true;
            this.activeItem = menu;
        }
        
        this._render();
    }
    
    /**
     * 获取当前活动菜单项
     * @returns {Object|null} 当前活动菜单
     */
    getActive() {
        return this.activeItem || this.menus.find(m => m.active);
    }
    
    /**
     * 销毁组件
     */
    destroy() {
        this.container.innerHTML = '';
        this.menus = [];
        this.activeItem = null;
    }
}

// ============ 预定义菜单配置工厂 ============

const MenuConfig = {
    /**
     * 创建基础工具菜单
     */
    createToolsMenu() {
        return [
            {
                page: 'index.html',
                label: 'JSON格式化',
                icon: '📋',
                active: false
            },
            {
                page: 'text-compare.html',
                label: '文字对比',
                icon: '⚖️',
                active: false
            }
        ];
    },
    
    /**
     * 创建带徽章的菜单项
     */
    createBadgeMenuItem(page, label, icon, badge, badgeColor) {
        return {
            page,
            label,
            icon,
            badge,
            badgeColor,
            onClick: (menu) => {
                console.log(`Menu clicked: ${menu.label}`);
            }
        };
    }
};

// 导出到全局
window.SidebarComponent = SidebarComponent;
window.MenuConfig = MenuConfig;
