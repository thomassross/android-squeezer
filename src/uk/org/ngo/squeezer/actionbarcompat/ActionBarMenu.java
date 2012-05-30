package uk.org.ngo.squeezer.actionbarcompat;

/**
 * This is a specialization of {@link SimpleMenu} with the necessary extensions
 * for our actionbar-compat purposes.
 * 
 * @author Kurt Aaholst <kaaholst@gmail.com>
 */
class ActionBarMenu extends SimpleMenu {
    final private ActionBarHelperBase mActionBarHelper;
    
    public ActionBarMenu(ActionBarHelperBase actionBarHelper) {
        super(actionBarHelper.mActivity);
        mActionBarHelper = actionBarHelper;
    }

    public ActionBarHelperBase getActionBarHelper() {
        return mActionBarHelper;
    }

   
}
