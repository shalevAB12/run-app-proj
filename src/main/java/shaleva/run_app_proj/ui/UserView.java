package shaleva.run_app_proj.ui;

import com.vaadin.flow.component.button.Button;
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
    public UserView(UserService userService) {
        this.userService = userService;

        add(new H1("UserView"));
        HorizontalLayout layout = new HorizontalLayout();
        layout.add(txfUn = new TextField("username: "));
        layout.add(txfPw = new TextField("password: "));
        layout.add(btnInsert = new Button("Insert user to DB"));
        btnInsert.addClickListener(clickEvent -> insertUserToDB());
        add(layout);
    }

    private void insertUserToDB() {
        String un = txfUn.getValue();
        String pw = txfPw.getValue();
        System.out.println("***********************************11");
        // validation check
        if (un == null || pw == null || un.length() < 6) {
            return;
        }

        try {
            userService.insertUser(new User(un, pw));
            Notification.show("User inserted OK", 3000, Position.MIDDLE);
        } catch (Exception e) {
            e.printStackTrace();
            // update user by notification for this error
            Notification.show("User NOT inserted", 3000, Position.MIDDLE);
        }
    }
}
