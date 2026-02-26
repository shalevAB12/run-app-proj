package shaleva.run_app_proj.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import shaleva.run_app_proj.datamodels.User;
import shaleva.run_app_proj.services.UserService;

@Route("/")
public class UserView extends VerticalLayout {
    private UserService userService;
    private TextField txfUn;
    private TextField txfPw;
    private Button btnInsert;
    private Grid<User> usersGrid;

    public UserView(UserService userService) {
        this.userService = userService;

        add(new H1("UserView"));
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setAlignItems(Alignment.BASELINE);
        this.usersGrid = new Grid<>(User.class);
        this.usersGrid.setItems(userService.getAllUsers());
        this.usersGrid.setColumns(new String[] { "username", "password" });
        this.usersGrid.getColumns().forEach(col -> col.setTextAlign(ColumnTextAlign.CENTER));
        this.usersGrid.setAllRowsVisible(true);

        layout.add(txfUn = new TextField("username"));
        layout.add(txfPw = new TextField("password"));
        layout.add(btnInsert = new Button("Insert user to DB"));
        layout.add(new Component[] { this.usersGrid });

        btnInsert.addClickListener(clickEvent -> insertUserToDB());
        add(layout);
    }

    // user insertion
    private void insertUserToDB() {
        String un = txfUn.getValue();
        String pw = txfPw.getValue();

        // validation check
        if (un == null || pw == null || un.length() < 6) {
            return;
        }

        try {
            userService.insertUser(new User(un, pw));
            this.usersGrid.setItems(this.userService.getAllUsers());

            // insertion has succeed
            Notification.show("User inserted OK", 3000, Position.MIDDLE);
        } catch (Exception e) {
            e.printStackTrace();
            // update user by notification for this error
            Notification.show("User NOT inserted", 3000, Position.MIDDLE);
        }
    }
}
