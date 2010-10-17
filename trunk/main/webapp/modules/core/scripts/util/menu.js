MenuSystem = {
    _layers: [],
    _overlay: null
};

MenuSystem.showMenu = function(elmt, onDismiss) {
    if (!MenuSystem._overlay) {
        MenuSystem._overlay = $('<div>&nbsp;</div>')
            .addClass("menu-overlay")
            .appendTo(document.body)
            .click(MenuSystem.dismissAll);
    }
    
    elmt.css("z-index", 1010 + MenuSystem._layers.length).appendTo(document.body);
    
    var layer = {
        elmt: elmt,
        onDismiss: onDismiss
    };
    MenuSystem._layers.push(layer);
    
    var level = MenuSystem._layers.length;
    
    return level;
};

MenuSystem.dismissAll = function() {
    MenuSystem.dismissUntil(0);
    if (MenuSystem._overlay !== null) {
        MenuSystem._overlay.remove();
        MenuSystem._overlay = null;
    }
};

MenuSystem.dismissUntil = function(level) {
    for (var i = MenuSystem._layers.length - 1; i >= level; i--) {
        var layer = MenuSystem._layers[i];
        layer.elmt.remove();
        layer.elmt.unbind();
        layer.onDismiss();
    }
    MenuSystem._layers = MenuSystem._layers.slice(0, level);
};

MenuSystem.createMenu = function() {
    return $('<div></div>').addClass("menu-container");
};

MenuSystem.createMenuItem = function() {
    return $('<a href="javascript:{}"></a>').addClass("menu-item");
};

MenuSystem.positionMenuAboveBelow = function(menu, elmt) {
    var offset = elmt.offset();
    var windowWidth = $(window).width();
    var windowHeight = $(window).height();
    
    if (offset.top + elmt.outerHeight() - document.body.scrollTop + menu.outerHeight() > windowHeight - 10) {
        menu.css("top", (offset.top - menu.outerHeight()) + "px");
    } else {
        menu.css("top", (offset.top + elmt.outerHeight()) + "px");
    }
    
    if (offset.left - document.body.scrollLeft + menu.outerWidth() > windowWidth - 10) {
        menu.css("left", (offset.left + elmt.outerWidth() - menu.outerWidth()) + "px");
    } else {
        menu.css("left", offset.left + "px");
    }
};

MenuSystem.positionMenuLeftRight = function(menu, elmt) {
    var offset = elmt.offset();
    var windowWidth = $(window).width();
    var windowHeight = $(window).height();
    
    if (offset.top - document.body.scrollTop + menu.outerHeight() > windowHeight - 10) {
        menu.css("top", (offset.top + elmt.outerHeight() - menu.outerHeight()) + "px");
    } else {
        menu.css("top", offset.top + "px");
    }
    
    if (offset.left + elmt.outerWidth() - document.body.scrollLeft + menu.outerWidth() > windowWidth - 10) {
        menu.css("left", Math.max(10, offset.left - menu.outerWidth()) + "px");
    } else {
        menu.css("left", (offset.left + elmt.outerWidth()) + "px");
    }
};

MenuSystem.createAndShowStandardMenu = function(items, elmt, options) {
    options = options || {
        horizontal: false
    };
    
    var menu = MenuSystem.createMenu();
    if ("width" in options) {
        menu.width(options.width);
    }
    
    var createMenuItem = function(item) {
        if ("label" in item) {
            var menuItem = MenuSystem.createMenuItem().appendTo(menu);
            if ("submenu" in item) {
                menuItem.html(
                    '<table width="100%" cellspacing="0" cellpadding="0" class="menu-item-layout"><tr>' +
                        '<td>' + item.label + '</td>' +
                        '<td width="1%"><img src="/images/right-arrow.png" /></td>' +
                    '</tr></table>'
                );
                menuItem.mouseenter(function() {
                    MenuSystem.dismissUntil(level);
                    
                    menuItem.addClass("menu-expanded");
                    
                    MenuSystem.createAndShowStandardMenu(
                        item.submenu, 
                        this,
                        {
                            horizontal: true,
                            onDismiss: function() {
                                menuItem.removeClass("menu-expanded");
                            }
                        }
                    );
                });
            } else {
                menuItem.html(item.label).click(function(evt) {
                    item.click.call(this, evt);
                    MenuSystem.dismissAll();
                });
                if ("tooltip" in item) {
                    menuItem.attr("title", item.tooltip);
                }
                menuItem.mouseenter(function() {
                    MenuSystem.dismissUntil(level);
                });
            }
        } else if ("heading" in item) {
            $('<div></div>').addClass("menu-section").text(item.heading).appendTo(menu);
        } else {
            $('<hr />').appendTo(menu);
        }
    };
    
    for (var i = 0; i < items.length; i++) {
        createMenuItem(items[i]);
    }
    
    var level = MenuSystem.showMenu(menu, "onDismiss" in options ? options.onDismiss : function(){});
    if (options.horizontal) {
        MenuSystem.positionMenuLeftRight(menu, $(elmt));
    } else {
        MenuSystem.positionMenuAboveBelow(menu, $(elmt));
    }
    
    return level;
};

MenuSystem.find = function(rootItems, path, levels) {
    var menuItems = rootItems;
    for (var p = 0; p < path.length && p < levels; p++) {
        var segment = path[p];
        var subMenuItems;
        
        for (var i = 0; i < menuItems.length; i++) {
            var menuItem = menuItems[i];
            if (menuItem.id == segment) {
                if ("submenu" in menuItem) {
                    subMenuItems = menuItem.submenu;
                } else {
                    return undefined;
                }
                break;
            }
        }
        
        if (subMenuItems) {
            menuItems = subMenuItems;
        } else {
            return undefined;
        }
    }
    
    return menuItems;   
};

MenuSystem.appendTo = function(rootItems, path, what) {
    var menuItems = MenuSystem.find(rootItems, path, path.length);
    if (menuItems) {
        if (what instanceof Array) {
            $.merge(menuItems, what);
        } else {
            menuItems.push(what);
        }
    }
};

MenuSystem.insertBefore = function(rootItems, path, what) {
    var menuItems = MenuSystem.find(rootItems, path, path.length - 1);
    if ((menuItems) && path.length > 0) {
        var spliceArgs = [ 0, 0 ];
        if (what instanceof Array) {
            $.merge(spliceArgs, what);
        } else {
            spliceArgs.push(what);
        }
        
        var segment = path[path.length - 1];
        for (var i = 0; i < menuItems.length; i++) {
            var menuItem = menuItems[i];
            if (menuItem.id == segment) {
                spliceArgs[0] = i;
                break;
            }
        }
        
        Array.prototype.splice.apply(menuItems, spliceArgs);
    }
};

MenuSystem.insertAfter = function(rootItems, path, what) {
    var menuItems = MenuSystem.find(rootItems, path, path.length - 1);
    if ((menuItems) && path.length > 0) {
        var spliceArgs = [ menuItems.length, 0 ];
        if (what instanceof Array) {
            $.merge(spliceArgs, what);
        } else {
            spliceArgs.push(what);
        }
        
        var segment = path[path.length - 1];
        for (var i = 0; i < menuItems.length; i++) {
            var menuItem = menuItems[i];
            if (menuItem.id == segment) {
                spliceArgs[0] = i + 1;
                break;
            }
        }
        
        Array.prototype.splice.apply(menuItems, spliceArgs);
    }
};