package ui.control.drawer.submenu;

import ui.control.drawer.GroupType;
import ui.control.drawer.SortType;
import ui.scene.MainScene;

/**
 * Created by LM on 10/02/2017.
 */
public final class SubMenuFactory {
    public static SubMenu createGroupBySubMenu(MainScene mainScene){
        SubMenu groupMenu = new SubMenu("groupBy");
        for (GroupType g : GroupType.values()) {
            TextItem item = new TextItem(g.getId());
            item.setOnAction(event -> {
                mainScene.groupBy(g);
                groupMenu.unselectAllItems();
                item.setSelected(true);
            });
            groupMenu.addItem(item);
        }
        return groupMenu;
    }

    public static SubMenu createSortBySubMenu(MainScene mainScene){
        SubMenu sortMenu = new SubMenu("sortBy");
        for (SortType s : SortType.values()) {
            TextItem item = new TextItem(s.getId());
            item.setOnAction(event -> {
                mainScene.sortBy(s);
                sortMenu.unselectAllItems();
                item.setSelected(true);
            });
            sortMenu.addItem(item);
        }
        return sortMenu;
    }
}
