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

        // Выводим доступные команды
        printCommands();

        while (true) {
            System.out.println("Введите команду (или 'exit' для выхода):");
            String command = scanner.nextLine();

            if (command.equalsIgnoreCase("exit")) {
                break;
            }

            handleCommand(command, scanner);
        }

        sessionFactory.close();
    }

    private static void printCommands() {
        System.out.println("Доступные команды:");
        System.out.println("/showProductsByPerson <имя_покупателя> - посмотреть какие товары покупал клиент");
        System.out.println("/findPersonsByProductTitle <название_товара> - какие клиенты купили определенный товар");
        System.out.println("/removePerson <имя_покупателя> - удалить покупателя из базы данных");
        System.out.println("/buy <имя_покупателя> <название_товара> - организовать покупку товара");
        System.out.println("/addProduct <название_товара> <стоимость> - добавить новый товар");
        System.out.println("/addCustomer <имя_покупателя> - добавить нового покупателя");
        System.out.println("Введите 'exit' для выхода из программы.");
    }

    private static void handleCommand(String command, Scanner scanner) {
        if (command.startsWith("/showProductsByPerson")) {
            String customerName = command.split(" ")[1];
            // Получаем ID покупателя по имени
            Long customerId = getCustomerIdByName(customerName);
            if (customerId != null) {
                showProductsByPerson(customerId);
            } else {
                System.out.println("Покупатель с именем " + customerName + " не найден.");
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
            System.out.println("Неизвестная команда");
        }
    }

    private static void showProductsByPerson(Long customerId) {
        try (Session session = sessionFactory.openSession()) {
            // Получаем покупателя по его ID
            Customer customer = session.get(Customer.class, customerId);

            if (customer == null) {
                System.out.println("Покупатель с ID " + customerId + " не найден.");
                return;
            }

            // Получаем список покупок через поле purchases
            List<Purchase> purchases = customer.getPurchases();

            if (purchases.isEmpty()) {
                System.out.println("У покупателя " + customer.getName() + " нет покупок.");
            } else {
                System.out.println("Товары купленные клиентом " + customer.getName() + ":");
                for (Purchase purchase : purchases) {
                    System.out.println("- " + purchase.getProduct().getTitle() + ", цена: " + purchase.getPurchasePrice());
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
                System.out.println("Клиенты не найдены для товара " + productTitle);
            } else {
                System.out.println("Клиенты купившие товар " + productTitle + ":");
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
                System.out.println("Клиент " + name + " успешно удалён.");
            } else {
                System.out.println("Клиент " + name + " не найден.");
            }
        }
    }

    private static void buy(String customerName, String productTitle) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            // Получаем покупателя
            Customer customer = session.createQuery("FROM Customer c WHERE c.name = :name", Customer.class)
                    .setParameter("name", customerName)
                    .uniqueResult();

            // Получаем продукт
            Product product = session.createQuery("FROM Product p WHERE p.title = :title", Product.class)
                    .setParameter("title", productTitle)
                    .uniqueResult();

            if (customer == null || product == null) {
                System.out.println("Покупатель или товар не найдены.");
                transaction.rollback();
                return;
            }

            // Создаём запись о покупке
            Purchase purchase = new Purchase();
            purchase.setCustomer(customer);
            purchase.setProduct(product);
            purchase.setPurchasePrice(product.getPrice());

            session.save(purchase);
            transaction.commit();

            System.out.println("Товар " + productTitle + " был успешно куплен клиентом " + customerName + " за " + product.getPrice());
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

            System.out.println("Товар " + title + " был успешно добавлен.");
        }
    }

    private static void addCustomer(String name) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            Customer customer = new Customer();
            customer.setName(name);

            session.save(customer);
            transaction.commit();

            System.out.println("Клиент " + name + " был успешно добавлен.");
        }
    }
    private static Long getCustomerIdByName(String customerName) {
        try (Session session = sessionFactory.openSession()) {
            // Находим покупателя по имени
            Customer customer = session.createQuery("FROM Customer c WHERE c.name = :name", Customer.class)
                    .setParameter("name", customerName)
                    .uniqueResult();

            // Возвращаем ID, если покупатель найден
            return customer != null ? customer.getId() : null;
        }
    }
}

