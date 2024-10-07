import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.util.List;
import java.util.Scanner;

public class Main {
    private static SessionFactory sessionFactory;

    public static void main(String[] args) {
        sessionFactory = new Configuration().configure().buildSessionFactory();
        Scanner scanner = new Scanner(System.in);

        // ������� ��������� �������
        printCommands();

        while (true) {
            System.out.println("������� ������� (��� 'exit' ��� ������):");
            String command = scanner.nextLine();

            if (command.equalsIgnoreCase("exit")) {
                break;
            }

            handleCommand(command, scanner);
        }

        sessionFactory.close();
    }

    private static void printCommands() {
        System.out.println("��������� �������:");
        System.out.println("/showProductsByPerson <���_����������> - ���������� ����� ������ ������� ������");
        System.out.println("/findPersonsByProductTitle <��������_������> - ����� ������� ������ ������������ �����");
        System.out.println("/removePerson <���_����������> - ������� ���������� �� ���� ������");
        System.out.println("/buy <���_����������> <��������_������> - ������������ ������� ������");
        System.out.println("/addProduct <��������_������> <���������> - �������� ����� �����");
        System.out.println("/addCustomer <���_����������> - �������� ������ ����������");
        System.out.println("������� 'exit' ��� ������ �� ���������.");
    }

    private static void handleCommand(String command, Scanner scanner) {
        if (command.startsWith("/showProductsByPerson")) {
            String customerName = command.split(" ")[1];
            // �������� ID ���������� �� �����
            Long customerId = getCustomerIdByName(customerName);
            if (customerId != null) {
                showProductsByPerson(customerId);
            } else {
                System.out.println("���������� � ������ " + customerName + " �� ������.");
            }
        } else if (command.startsWith("/findPersonsByProductTitle")) {
            String productTitle = command.split(" ")[1];
            findPersonsByProductTitle(productTitle);
        } else if (command.startsWith("/removePerson")) {
            String name = command.split(" ")[1];
            removePerson(name);
        } else if (command.startsWith("/buy")) {
            String[] parts = command.split(" ");
            String customerName = parts[1];
            String productTitle = parts[2];
            buy(customerName, productTitle);
        } else if (command.startsWith("/addProduct")) {
            String[] parts = command.split(" ");
            String productTitle = parts[1];
            double price = Double.parseDouble(parts[2]);
            addProduct(productTitle, price);
        } else if (command.startsWith("/addCustomer")) {
            String customerName = command.split(" ")[1];
            addCustomer(customerName);
        } else {
            System.out.println("����������� �������");
        }
    }

    private static void showProductsByPerson(Long customerId) {
        try (Session session = sessionFactory.openSession()) {
            // �������� ���������� �� ��� ID
            Customer customer = session.get(Customer.class, customerId);

            if (customer == null) {
                System.out.println("���������� � ID " + customerId + " �� ������.");
                return;
            }

            // �������� ������ ������� ����� ���� purchases
            List<Purchase> purchases = customer.getPurchases();

            if (purchases.isEmpty()) {
                System.out.println("� ���������� " + customer.getName() + " ��� �������.");
            } else {
                System.out.println("������ ��������� �������� " + customer.getName() + ":");
                for (Purchase purchase : purchases) {
                    System.out.println("- " + purchase.getProduct().getTitle() + ", ����: " + purchase.getPurchasePrice());
                }
            }
        }
    }

    private static void findPersonsByProductTitle(String productTitle) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "SELECT c FROM Purchase p JOIN p.customer c JOIN p.product prod WHERE prod.title = :productTitle";
            List<Customer> customers = session.createQuery(hql, Customer.class)
                    .setParameter("productTitle", productTitle)
                    .getResultList();

            if (customers.isEmpty()) {
                System.out.println("������� �� ������� ��� ������ " + productTitle);
            } else {
                System.out.println("������� �������� ����� " + productTitle + ":");
                for (Customer customer : customers) {
                    System.out.println("- " + customer.getName());
                }
            }
        }
    }

    private static void removePerson(String name) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            String hql = "DELETE FROM Customer c WHERE c.name = :name";
            int deletedCount = session.createQuery(hql)
                    .setParameter("name", name)
                    .executeUpdate();
            transaction.commit();

            if (deletedCount > 0) {
                System.out.println("������ " + name + " ������� �����.");
            } else {
                System.out.println("������ " + name + " �� ������.");
            }
        }
    }

    private static void buy(String customerName, String productTitle) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            // �������� ����������
            Customer customer = session.createQuery("FROM Customer c WHERE c.name = :name", Customer.class)
                    .setParameter("name", customerName)
                    .uniqueResult();

            // �������� �������
            Product product = session.createQuery("FROM Product p WHERE p.title = :title", Product.class)
                    .setParameter("title", productTitle)
                    .uniqueResult();

            if (customer == null || product == null) {
                System.out.println("���������� ��� ����� �� �������.");
                transaction.rollback();
                return;
            }

            // ������ ������ � �������
            Purchase purchase = new Purchase();
            purchase.setCustomer(customer);
            purchase.setProduct(product);
            purchase.setPurchasePrice(product.getPrice());

            session.save(purchase);
            transaction.commit();

            System.out.println("����� " + productTitle + " ��� ������� ������ �������� " + customerName + " �� " + product.getPrice());
        }
    }

    private static void addProduct(String title, double price) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            Product product = new Product();
            product.setTitle(title);
            product.setPrice(price);

            session.save(product);
            transaction.commit();

            System.out.println("����� " + title + " ��� ������� ��������.");
        }
    }

    private static void addCustomer(String name) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            Customer customer = new Customer();
            customer.setName(name);

            session.save(customer);
            transaction.commit();

            System.out.println("������ " + name + " ��� ������� ��������.");
        }
    }
    private static Long getCustomerIdByName(String customerName) {
        try (Session session = sessionFactory.openSession()) {
            // ������� ���������� �� �����
            Customer customer = session.createQuery("FROM Customer c WHERE c.name = :name", Customer.class)
                    .setParameter("name", customerName)
                    .uniqueResult();

            // ���������� ID, ���� ���������� ������
            return customer != null ? customer.getId() : null;
        }
    }
}

