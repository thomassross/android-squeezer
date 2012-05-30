package uk.org.ngo.squeezer.actionbarcompat;

import android.view.MenuItem;

/**
 * This is a specialization of {@link SimpleMenuItem} with the necessary extensions
 * for our actionbar-compat purposes.
 * 
 * @author Kurt Aaholst <kaaholst@gmail.com>
 */
class ActionBarMenuItem extends SimpleMenuItem {
    private ActionBarMenu mMenu;

    public ActionBarMenuItem(ActionBarMenu menu, int id, int order, CharSequence title) {
        super(menu.getContext(), id, order, title);
        mMenu = menu;
    }

    @Override
    public MenuItem setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mMenu.getActionBarHelper().setEnabled(this, enabled);
        return this;
    }

    @Override
    public MenuItem setVisible(boolean b) {
        super.setVisible(b);
        mMenu.getActionBarHelper().setVisible(this, b);
        return this;
    }

}
