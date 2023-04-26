package com.example.softengproject.controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;

import org.hibernate.JDBCException;
import org.hibernate.exception.internal.SQLExceptionTypeDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import com.example.softengproject.entity.Product;
import com.example.softengproject.entity.ShoppingCart;
import com.example.softengproject.entity.Product.Type;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.RequestParam;


/* 
 * A simple controller class must do the following:
 * Handle HTTP GET requests where the request path is /demo
 * Build a list of DemoModelAgain objects of DemoModel
 * Hand the request and the data off to a view template to be rendered as HTML
 * and sent to the request web browser
 */

@Controller
@Slf4j
@RequestMapping("/")
public class MainController {

    @Autowired private JdbcTemplate jdbcTemplate;

    Product product;

    public ShoppingCart shoppingCart;

    @PostConstruct
    public void initialize() throws Exception {
        this.shoppingCart = new ShoppingCart(loadShoppingCartProductList(), 0.00, 0.00, 0.00, 0.00, "null");
    }

    @RequestMapping("/403") 
    public String accessDenied() {
        return "/403";
    }

    @RequestMapping(value = "/", method = RequestMethod.GET) 
    public String showHomeTemplate() throws Exception {
        return "home";
    }

    /**
     * Fetches data from Invoice template
     *
     * @param product [TODO:description]
     * @param model [TODO:description]
     *
     * @return [TODO:description]
     *
     * @throws Exception [TODO:description]
     */
    @RequestMapping(value="/invoice", method=RequestMethod.GET)
    public String goToCheckout(@ModelAttribute Product product, Model model) throws Exception {
        return "invoice";
    }

    
    /**
     * Sends submitted data to Invoice
     *
     * @param product [TODO:description]
     * @param model [TODO:description]
     *
     * @return [TODO:description]
     *
     * @throws Exception [TODO:description]
     */
    @RequestMapping(value="/invoice", method=RequestMethod.POST)
    public String submitCheckout(@ModelAttribute Product product, Model model) throws Exception {
        

        return "invoice";
    }


    /*
     * Fetch index of item
     * Get the values from the item
     * Add the product object to array list of products in shopping cart class
     * Populate csv file with product data
     * Load shopping-cart.html with shopping-cart.csv data
     * Subtract 1 from product quantity in desktops.html
     * Give notification that item has been added to cart
     */
    @RequestMapping(value={"/desktops", "/laptops", "/phones", "/accessories"}, method=RequestMethod.POST)
    public String addProductToShoppingCart(
            @ModelAttribute Product productDTO, Model model, 
            final RedirectAttributes redirectAttributes) throws Exception { 

        ArrayList<Product> loadProductList = new ArrayList<Product>();
        String redirectPage = "";

        System.out.println("\nAdding product to cart: \n\n"
                + "ID: " + productDTO.getId() + "\n"
                + "Name: " + productDTO.getName() + "\n"
                + "Type: " + productDTO.getType() + "\n"
                + "Description: " + productDTO.getDescription() + "\n"
                + "Price: " + productDTO.getPrice() + "\n"
                + "Quantity: " + productDTO.getQuantity() + "\n"
                + "Vendor: " + productDTO.getVendor() + "\n"
                + "Rating: " + productDTO.getRating() + "/5\n");

        switch (productDTO.getType().toString()) {
            case "DESKTOP":
                redirectPage = "desktops";
                loadProductList = loadProductTypeDesktopList();
                break;
            case "LAPTOP":
                redirectPage = "laptops";
                loadProductList = loadProductTypeLaptopList(); 
                break;
            case "PHONE":
                redirectPage = "phones";
                loadProductList = loadProductTypePhoneList();
                break;
            case "ACCESSORY":
                redirectPage = "accessories";
                loadProductList = loadProductTypeAccessoriesList();
                break;
            default:
                System.err.println("\nERROR: Product TYPE could not be determined in MainController\n\n");
                break;
        }

        System.out.println("Product of type "+productDTO.getType().toString()); 

        this.shoppingCart.getProducts().add(productDTO);
        redirectAttributes.addFlashAttribute(productDTO);
        model.addAttribute("productList", loadProductList);
        model.addAttribute("productDTO", product);
        model.addAttribute("shoppingCartProductPrice", Double.toString(getTotalShoppingCartPrice()));
        model.addAttribute("shoppingCartProductListQuantity", Integer.toString(getTotalShoppingCartProductQuantity()));
        model.addAttribute("shoppingCartProductTotalPrice", Double.toString(getTotalShoppingCartPrice() * getTotalShoppingCartProductQuantity()));

        appendShoppingCart(productDTO);

        return redirectPage;
            }

    @RequestMapping(value="/shopping-cart", method=RequestMethod.POST) 
    public String removeProductFromShoppingCart(
            @ModelAttribute Product productRemoved, Model model,
            final RedirectAttributes redirectAttributes) throws Exception {

        System.out.println("\nRemoving "+productRemoved.getId()+" from shopping cart.\n");

        // this.shoppingCart.getProducts().remove(productRemoved);
        // redirectAttributes.addFlashAttribute(productRemoved);
        model.addAttribute("productRemoved", productRemoved);
        model.addAttribute("shoppingCartTotalPrice", Double.toString(getTotalShoppingCartPrice()));
        model.addAttribute("shoppingCartProductListQuantity", Integer.toString(getTotalShoppingCartProductQuantity()));
        model.addAttribute("shoppingCartProductList", loadShoppingCartProductList());
        model.addAttribute("shoppingCartProductTotalPrice", Double.toString(getTotalShoppingCartPrice() * getTotalShoppingCartProductQuantity()));
        removeProductFromShoppingCart(productRemoved);
        return "shopping-cart";
            }

    /*
     * This is where the data will be rendered into the Thymeleaf template
     * this method sends a GET request to render the new data from the Model
     */
    /*
       @RequestMapping(value = "/desktops", method = RequestMethod.GET)
       public String showDesktopTemplate(Model model) throws Exception {
       model.addAttribute("productList", loadProductTypeDesktopList());
       model.addAttribute("productDTO", product); 
       return "desktops";
       }
       */
    //New
    @RequestMapping(value = "/desktops", method = RequestMethod.GET)
    public String showDesktopTemplate(Model model, @RequestParam(name="searchTerm", required=false) String searchTerm, 
            @RequestParam(name="priceRange", required=false) String priceRange, 
            @RequestParam(name="rating", required=false) String rating,
            @RequestParam(name="sortOrder", required=false) String sortOrder) throws Exception {
            ArrayList<Product> productList = loadProductTypeDesktopList();


            if (sortOrder != null && !sortOrder.isEmpty()) {
                switch (sortOrder) {
                    case "nameAZ":
                        productList.sort(Comparator.comparing(Product::getName));
                        break;
                    case "nameZA":
                        productList.sort(Comparator.comparing(Product::getName).reversed());
                        break;
                    case "vendorAZ":
                        productList.sort(Comparator.comparing(Product::getVendor));
                        break;
                    case "vendorZA":
                        productList.sort(Comparator.comparing(Product::getVendor).reversed());
                        break;
                    case "priceLtoH":
                        productList.sort(Comparator.comparing(Product::getPrice));
                        break;
                    case "priceHtoL":
                        productList.sort(Comparator.comparing(Product::getPrice).reversed());
                        break;
                    default:
                        break;
                }
            }
            if (searchTerm != null && !searchTerm.isEmpty()) {
                productList = searchProductList(productList, searchTerm);
            }
            if (priceRange != null && !priceRange.isEmpty()) {
                int minPrice = 0;
                int maxPrice = Integer.MAX_VALUE;
                switch (priceRange) {
                    case "1":
                        maxPrice = 200;
                        break;
                    case "2":
                        minPrice = 200;
                        maxPrice = 400;
                        break;
                    case "3":
                        minPrice = 400;
                        maxPrice = 700;
                        break;
                    case "4":
                        minPrice = 700;
                        maxPrice = 1000;
                        break;
                    case "5":
                        minPrice = 1000;
                        break;
                }
                productList = filterProductListByPrice(productList, minPrice, maxPrice);
            }
            if (rating != null && !rating.isEmpty()) {
                productList = filterProductListByRating(productList, Integer.parseInt(rating));
            }

            model.addAttribute("productList", productList);
            model.addAttribute("productDTO", product); 

            return "desktops";
    }

    /* Searches through Product List if search button is enabled */
    private ArrayList<Product>  searchProductList(ArrayList<Product> productList, String searchTerm) {
        ArrayList<Product>  searchResults = new ArrayList<>();
        for (Product product : productList) {
            if (product.getName().toLowerCase().contains(searchTerm.toLowerCase()) || 
                    product.getDescription().toLowerCase().contains(searchTerm.toLowerCase()) || 
                    product.getVendor().toLowerCase().contains(searchTerm.toLowerCase())) 
            {
                searchResults.add(product);
            }

        }
        return searchResults;
    }

    private ArrayList<Product>  filterProductListByRating(ArrayList<Product> productList, int rating) {
        ArrayList<Product>  searchResults = new ArrayList<>();
        for (Product product : productList) {
            if (product.getRating()>=rating)
            {
                searchResults.add(product);
            }

        }
        return searchResults;
    }
    private ArrayList<Product>  filterProductListByPrice(ArrayList<Product> productList, int minPrice, int maxPrice) {

        final int minP = minPrice;
        final int maxP = maxPrice;

        return productList.stream()
            .filter(p -> p.getPrice() >= minP && p.getPrice() <= maxP)
            .collect(Collectors.toCollection(ArrayList::new));

    }

    @RequestMapping(value = "/laptops", method = RequestMethod.GET)
    public String showLaptopTemplate(Model model) throws Exception {
        model.addAttribute("productList", "Laptop products load here");
        return "laptops";
    } 

    @RequestMapping(value = "/phones", method = RequestMethod.GET)
    public String showPhoneTemplate(Model model) throws Exception {
        model.addAttribute("productList", "Phone products load here");
        return "phones";
    }

    @RequestMapping(value = "/accessories", method = RequestMethod.GET)
    public String showAccessoriesTemplate(Model model) throws Exception {
        model.addAttribute("productList", "Accessory products load here");
        return "accessories";
    }

    @RequestMapping(value = "/shopping-cart", method = RequestMethod.GET)
    public String showShoppingCartTemplate(Model model) throws Exception {
        model.addAttribute("shoppingCartProductList", loadShoppingCartProductList());
        model.addAttribute("productRemoved", product);
        model.addAttribute("shoppingCartTotalPrice", Double.toString(getTotalShoppingCartPrice()));
        model.addAttribute("shoppingCartProductListQuantity", Integer.toString(getTotalShoppingCartProductQuantity()));
        model.addAttribute("shoppingCartProductTotalPrice", Double.toString(getTotalShoppingCartPrice() * getTotalShoppingCartProductQuantity()));
        return "shopping-cart";
    }

    @RequestMapping(value = "/home")
    public String redirectToAboutTemplate(Model model) throws Exception {
        return "home";
    }

    /**
     * Loads data from desktops.csv into an ArrayList of Product objects
     *
     * @return ArrayList<Product> productList
     *
     * @throws Exception 
     */
    @ModelAttribute
    private ArrayList<Product> loadProductTypeDesktopList() throws Exception {
        ArrayList<Product> productList = new ArrayList<Product>();
        String filename = "src/main/java/com/example/softengproject/data/desktops.csv";
        try {
            productList = readCSVFile(productList, filename);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return productList;
    }

    @ModelAttribute
    private ArrayList<Product> loadProductTypeLaptopList() throws Exception {
        ArrayList<Product> productList = new ArrayList<Product>();
        String filename = "src/main/java/com/example/softengproject/data/laptops.csv";
        try {
            productList = readCSVFile(productList, filename);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return productList;
    }

    @ModelAttribute
    private ArrayList<Product> loadProductTypePhoneList() throws Exception{
        ArrayList<Product> productList = new ArrayList<Product>();
        String filename = "src/main/java/com/example/softengproject/data/phones.csv";
        try {
            productList = readCSVFile(productList, filename);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return productList;
    }

    @ModelAttribute
    private ArrayList<Product> loadProductTypeAccessoriesList() throws Exception{
        ArrayList<Product> productList = new ArrayList<Product>();
        String filename = "src/main/java/com/example/softengproject/data/accessories.csv";
        try {
            productList = readCSVFile(productList, filename);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return productList;
    }

    @ModelAttribute
    private List<Product> loadShoppingCartProductList() throws Exception {
        List<Product> list = new ArrayList<Product>();
        String sqlQuery = "SELECT * FROM Shopping_Cart_Items";
        try {
            // productList = readCSVFile(productList, filename);
            // product_id | name       | type    | description  | price | quantity | vendor      | rating | quantity_in_cart
            list = jdbcTemplate.query(sqlQuery, 
                    new Object[] {},
                    new RowMapper<Product>() {
                        public Product mapRow(ResultSet rs, int rowNum) throws SQLException {
                            Product p = new Product();
                            p.setId(Integer.parseInt(rs.getString(1)));
                            p.setName(rs.getString(2));
                            //p.setType(rs.getString(3).valueOf("DESKTOP"));
                            switch (rs.getString(3)) {
                                case "DESKTOP":
                                    p.setType(rs.getString(3).valueOf("DESKTOP"));
                                    break;
                                case "LAPTOP":
                                    p.setType(rs.getString(3).valueOf("LAPTOP"));
                                    break;
                                case "PHONE":
                                    p.setType(rs.getString(3).valueOf("PHONE"));
                                    break;
                                case "ACCESSORY":
                                    p.setType(rs.getString(3).valueOf("ACCESSORY"));
                                    break;
                                default:
                                    System.err.println("\nERROR: No type could be accessed in loadShoppingCartProductList\n\n");
                                    break;
                            }
                            p.setDescription(rs.getString(4));
                            p.setPrice(rs.getDouble(5));
                            p.setQuantity(rs.getInt(6));
                            p.setVendor(rs.getString(7));
                            p.setRating(rs.getInt(8));
                            p.setShoppingCartQuantity(rs.getInt(9));

                            return p; 
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list; 
    }

    /**
     * Reads product CSV files line by line and stores the info into an array of
     * Product objects
     *
     * @param filename The file name to best read
     *
     * @return An array list of Product objects with updated info
     */
    private ArrayList<Product> readCSVFile(ArrayList<Product> productList, String filename) {
        File readFileObj = new File(filename);
        String line = "";
        String splitBy = ",";

        try (BufferedReader reader = new BufferedReader(
                    new FileReader(readFileObj))) {
            System.out.println("Reading file: " + filename);
            // reader.readLine();
            while ( (line = reader.readLine()) != null) {
                String[] columns = line.split(splitBy);

                Integer id = Integer.parseInt(columns[0]);
                String name = columns[1].replaceAll("\"", "");
                Type type = Type.valueOf(columns[2].replaceAll("\\s+", "").replaceAll("\"", ""));
                String description = columns[3].replaceAll("\"", "");
                Double price = Double.parseDouble(columns[4]);
                Integer quantity = Integer.parseInt(columns[5]);
                String vendor = columns[6].replaceAll("\"", "");
                Integer rating = Integer.parseInt(columns[7]);

                productList.add(new Product(id, name, type, description, price, quantity, vendor, rating));
            }

        } catch (FileNotFoundException e1) {
            // e1.printStackTrace();
            System.err.println("\n" + filename + " not found\n");
            System.out.println(new File(".").getAbsoluteFile());
            System.exit(0);
        } catch (IOException e2) {
            e2.printStackTrace();
            System.err.println(filename + " is not read");
        }

        return productList;
    }

    /**
     * Appends shopping-cart.csv with Product data
     *
     * @param product Product object
     *
     * @throws IOException Input/Output exception
     */
    private void appendShoppingCart(Product product) 
            throws JDBCException {

            String sqlQueryAddProductRow = 
                "INSERT INTO Shopping_Cart_Items (product_id, name, type, description, price, quantity, vendor, rating, quantity_in_cart) VALUES (?,?,?,?,?,?,?,?,quantity_in_cart=quantity_in_cart+1)";
            String sqlQueryAddProductQuantity = 
                "UPDATE Shopping_Cart_Items SET quantity_in_cart=quantity_in_cart+1 WHERE product_id=?";
            Integer result = 0;

            try {
                if (productExistsInShoppingCart(product)) {
                    result = jdbcTemplate.update(sqlQueryAddProductQuantity, product.getId().toString());
                } else {
                    result = jdbcTemplate.update(sqlQueryAddProductRow,
                            product.getId().toString(), product.getName(), product.getType().toString(),
                            product.getDescription(), product.getPrice().toString(), product.getQuantity().toString(),
                            product.getVendor(), product.getRating());
                    result = jdbcTemplate.update(sqlQueryAddProductQuantity, product.getId().toString());
                }

                if (result > 0) {
                    System.out.println("\n\n\nA new row has been inserted\n\n\n");
                }
            } catch (JDBCException e) {
                e.printStackTrace();
            }
    }

    /**
     * [TODO:description]
     *
     * @param product [TODO:description]
     *
     * @throws JDBCException [TODO:description]
     */
    private void removeProductFromShoppingCart(Product product) throws JDBCException {

        try {
            String sqlQueryDeleteRow = "DELETE FROM Shopping_Cart_Items WHERE product_id = ?";
            String sqlQuerySubtractFromQuantity = "UPDATE Shopping_Cart_Items SET quantity_in_cart=quantity_in_cart-1 WHERE product_id=?";
            String sqlQueryGetProductQuantity = "SELECT quantity_in_cart FROM Shopping_Cart_Items WHERE product_id=?";
            Integer result = 0;
            Integer count = 0;

            if (productExistsInShoppingCart(product)) {
                result = jdbcTemplate.update(sqlQuerySubtractFromQuantity, product.getId().toString());
                count = jdbcTemplate.queryForObject(sqlQueryGetProductQuantity, new Object[] { product.getId().toString() }, Integer.class);
                if (count.equals(0)) {
                    result = jdbcTemplate.update(sqlQueryDeleteRow, product.getId().toString());
                    return;
                }
                return;
            } else {
                result = jdbcTemplate.update(sqlQueryDeleteRow, product.getId().toString());
                return;
            }
        } catch (JDBCException e) {
            e.printStackTrace();
        }

        System.err.println("\nERROR: removeProductFromShoppingCart() did NOT meet any of the conditions!\n\n");
        System.exit(-1);

    }

    /**
     * [TODO:description]
     *
     * @param product [TODO:description]
     *
     * @return [TODO:description]
     */
    private Boolean productExistsInShoppingCart(Product product) {
        String sqlQuerySelectAll = "SELECT COUNT(*) FROM Shopping_Cart_Items WHERE product_id = ?";
        Boolean exists = false;
        Integer count = jdbcTemplate.queryForObject(sqlQuerySelectAll, new Object[] { product.getId().toString() }, Integer.class);
        return exists = count > 0;
    }

    @ModelAttribute
    public Integer getTotalShoppingCartProductQuantity() {
        String sqlQuery = "SELECT SUM(quantity_in_cart) FROM Shopping_Cart_Items";

        Integer sum = 0;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/CampusTech",
                    "root", "praisethesun!!!");
            Statement st = con.createStatement();
            ResultSet res = st.executeQuery(sqlQuery);
            while (res.next()) {
                Integer c = res.getInt(1);
                sum = sum + c;
            }
            System.out.println("Sum of column = " + sum);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }
        return sum;
    }

    @ModelAttribute 
    public Double getTotalShoppingCartPrice() {
        String sqlQuery = "SELECT SUM(price) FROM Shopping_Cart_Items";

        Double totalPrice = 0.0;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/CampusTech",
                    "root", "praisethesun!!!");
            Statement st = con.createStatement();
            ResultSet res = st.executeQuery(sqlQuery);
            while (res.next()) {
                Double c = res.getDouble(1);
                totalPrice = totalPrice + c;
            }
            System.out.println("Sum of column = " + totalPrice);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }
        return totalPrice;
    }


}
