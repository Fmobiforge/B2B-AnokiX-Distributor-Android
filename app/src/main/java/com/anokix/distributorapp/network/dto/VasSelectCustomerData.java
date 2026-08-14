package com.anokix.distributorapp.network.dto;

/**
 * {@code data} block of {@code POST api/common/vas/select-customer}. Limes issues its token
 * per customer, so the Subscription, Catalog &amp; orders and Dynamic services tabs first
 * authenticate as the customer being served; {@code subject} is the Limes login that answered
 * and is what the "Acting as" chip shows once approved.
 */
public class VasSelectCustomerData {
    public boolean authenticated;
    public String subject;
    public Customer customer;

    public static class Customer {
        public String id;
        public String firstname;
        public String lastname;
        public String email;
        public String phone;
        public String status;
        public String limes_account_id;
    }

    /** The chip label: the Limes subject when it came back, otherwise the customer's email. */
    public String subjectLabel() {
        if (subject != null && !subject.trim().isEmpty()) return subject.trim();
        if (customer != null && customer.email != null && !customer.email.trim().isEmpty()) {
            return customer.email.trim();
        }
        return "";
    }
}
