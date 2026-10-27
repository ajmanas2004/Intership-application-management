package com.internshipmanagement;

import com.internshipmanagement.config.DatabaseConnection;
import com.internshipmanagement.config.ConnectionProvider;
import com.internshipmanagement.dao.*;
import com.internshipmanagement.service.*;
import com.internshipmanagement.view.*;
import javax.swing.*;
public final class Main {
    private Main() {}
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
                MainFrame frame = createFrame(new DatabaseConnection());
                frame.setVisible(true);
                frame.showScreen("dashboard");
            } catch (Exception e) { Ui.error(null, e); }
        });
    }

    /** Composition root: shared contracts are connected here, outside the views. Call on EDT. */
    public static MainFrame createFrame(ConnectionProvider connections) {
        CompanyDAO companyDao = new CompanyDAO(connections);
        InternshipDAO internshipDao = new InternshipDAO(connections);
        ApplicationDAO applicationDao = new ApplicationDAO(connections);
        CompanyService companies = new CompanyService(companyDao);
        InternshipService internships = new InternshipService(internshipDao, companyDao);
        ApplicationService applications = new ApplicationService(applicationDao, internshipDao);
        MainFrame frame = new MainFrame();
        frame.addScreen("dashboard", "Tổng quan", new DashboardPanel(new DashboardService(applicationDao)));
        frame.addScreen("companies", "Công ty", new CompanyPanel(companies));
        frame.addScreen("internships", "Vị trí thực tập", new InternshipPanel(internships, companies));
        frame.addScreen("applications", "Đơn ứng tuyển", new ApplicationPanel(applications, internships, new ExportService(), companies));
        return frame;
    }
}

