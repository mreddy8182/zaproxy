/*
 * Zed Attack Proxy (ZAP) and its related class files.
 * 
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 * 
 * Copyright 2013 The ZAP Development Team
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
package org.zaproxy.zap.extension.stdmenus;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;

import org.apache.log4j.Logger;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.extension.ExtensionPopupMenuItem;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.model.Session;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.extension.ExtensionPopupMenu;
import org.zaproxy.zap.extension.userauth.ContextUserAuthManager;
import org.zaproxy.zap.extension.userauth.ExtensionUserAuthentication;
import org.zaproxy.zap.model.Context;
import org.zaproxy.zap.userauth.User;
import org.zaproxy.zap.view.PopupMenuHistoryReference;

/**
 * The Class PopupUserMenuItemHolder is used as a holder for multiple {@link PopupUserMenu}.
 * Depending on the initialization, it can be shown by itself containing the Popup Menus for each
 * User or it can just place the Popup Menus in its parent.
 */
public abstract class PopupUserMenuItemHolder extends ExtensionPopupMenu {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4454384312614225721L;

	/** The parent's name. */
	private String parentName;

	/**
	 * The sub menus. Used only in the case it is not visible itself, to keep a reference to what
	 * popup menus it has added in the parent, so it can dinamically update them before each show.
	 */
	private List<ExtensionPopupMenuItem> subMenuItems = null;

	/** Whether it is visible itself. */
	private boolean visibleItself;

	/** The user authentication extension. */
	private ExtensionUserAuthentication extensionUserAuth;

	/**
	 * Instantiates a new popup user menu item holder. This initializes the holder so that the Popup
	 * Menus for each User are shown as submenus of this Holder.
	 * 
	 * @param label the label
	 * @param parentName the parent menu's name
	 */
	public PopupUserMenuItemHolder(String label, String parentName) {
		super(label);
		this.parentName = parentName;
		this.visibleItself = true;
		// Check whether the User Authentication extension is enabled
		extensionUserAuth = (ExtensionUserAuthentication) Control.getSingleton().getExtensionLoader()
				.getExtension(ExtensionUserAuthentication.NAME);
		if (extensionUserAuth == null || !extensionUserAuth.isEnabled()) {
			Logger.getLogger(PopupUserMenuItemHolder.class).warn(
					ExtensionUserAuthentication.class
							+ " is not enabled but is required for getting info about Users.");
			extensionUserAuth = null;
		}
	}

	/**
	 * Instantiates a new popup user menu item holder. This initializes the holder so that the Popup
	 * Menus for each User are shown as submenus of the parent, the holder not being visible.
	 * 
	 * @param parentName the parent menu's name
	 */
	public PopupUserMenuItemHolder(String parentName) {
		super("UserMenuItemHolder");
		this.parentName = parentName;
		this.visibleItself = false;
	}

	@Override
	public String getParentMenuName() {
		return this.parentName;
	}

	@Override
	public int getParentMenuIndex() {
		return 0;
	}

	@Override
	public boolean isSubMenu() {
		return true;
	}

	/**
	 * Gets the submenu items.
	 * 
	 * @return the submenu items
	 */
	private List<ExtensionPopupMenuItem> getSubmenuItems() {
		if (subMenuItems == null)
			subMenuItems = new ArrayList<ExtensionPopupMenuItem>();
		return subMenuItems;
	}

	@Override
	public boolean isEnableForComponent(Component invoker) {
		if (extensionUserAuth == null) {
			return false;
		}
		if (visibleItself)
			return super.isEnableForComponent(invoker);
		else {
			for (JMenuItem item : subMenuItems) {
				if (item instanceof PopupMenuHistoryReference) {
					PopupMenuHistoryReference itemRef = (PopupMenuHistoryReference) item;
					itemRef.isEnableForComponent(invoker);
				}
			}
			return false;
		}
	}

	@Override
	public void prepareShow() {
		// Remove existing popup menu items
		if (visibleItself)
			this.removeAll();
		else {
			for (ExtensionPopupMenuItem menu : getSubmenuItems()) {
				View.getSingleton().getPopupMenu().removeMenu(menu);

			}
			subMenuItems.clear();
		}

		// Add a popup menu item for each existing users
		Session session = Model.getSingleton().getSession();
		List<Context> contexts = session.getContexts();
		for (Context context : contexts) {
			ContextUserAuthManager manager = extensionUserAuth.getContextUserAuthManager(context.getIndex());

			for (User user : manager.getUsers()) {
				ExtensionPopupMenuItem piicm;
				if (visibleItself) {
					piicm = getPopupUserMenu(context, user, this.getText());
					this.add(piicm);
				} else {
					piicm = getPopupUserMenu(context, user, this.parentName);
					piicm.setMenuIndex(this.getMenuIndex());
					View.getSingleton().getPopupMenu().addMenu(piicm);
					subMenuItems.add(piicm);
				}
			}
		}
	}

	/**
	 * Gets the {@link PopupUserMenu} associated with a particular user from a particular context.
	 *
	 * @param context the context
	 * @param user the user
	 * @param parentName the parent menu's name
	 * @return the popup context menu
	 */
	public abstract PopupUserMenu getPopupUserMenu(Context context, User user, String parentName);
}
