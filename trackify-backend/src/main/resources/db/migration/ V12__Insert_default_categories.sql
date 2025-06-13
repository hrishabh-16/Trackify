-- Insert default system categories that all users can access
-- These categories are marked as system categories and cannot be deleted

INSERT INTO categories (
    name, 
    description, 
    color, 
    icon, 
    is_active, 
    is_system, 
    created_by, 
    team_id, 
    sort_order
) VALUES 
-- Essential categories that most users will need
('Food & Dining', 'Restaurant meals, groceries, takeout, food delivery', '#FF6B6B', '🍽️', TRUE, TRUE, 1, NULL, 1),
('Transportation', 'Gas, public transport, ride-sharing, parking, car maintenance', '#4ECDC4', '🚗', TRUE, TRUE, 1, NULL, 2),
('Shopping', 'Clothing, electronics, general purchases, online shopping', '#45B7D1', '🛍️', TRUE, TRUE, 1, NULL, 3),
('Bills & Utilities', 'Electricity, water, internet, phone, rent, mortgage', '#DDA0DD', '💡', TRUE, TRUE, 1, NULL, 4),
('Entertainment', 'Movies, games, subscriptions, concerts, events', '#96CEB4', '🎬', TRUE, TRUE, 1, NULL, 5),
('Health & Medical', 'Doctor visits, pharmacy, fitness, insurance, medical equipment', '#FFEAA7', '🏥', TRUE, TRUE, 1, NULL, 6),
('Travel', 'Flights, hotels, vacation expenses, travel insurance', '#98D8C8', '✈️', TRUE, TRUE, 1, NULL, 7),
('Education', 'Books, courses, school fees, training, certifications', '#F7DC6F', '📚', TRUE, TRUE, 1, NULL, 8),
('Personal Care', 'Haircuts, cosmetics, spa, gym membership, personal items', '#BB8FCE', '💄', TRUE, TRUE, 1, NULL, 9),
('Home & Garden', 'Furniture, appliances, home improvement, gardening', '#85C1E9', '🏠', TRUE, TRUE, 1, NULL, 10),
('Insurance', 'Life insurance, health insurance, auto insurance', '#F8C471', '🛡️', TRUE, TRUE, 1, NULL, 11),
('Investments', 'Stocks, bonds, mutual funds, retirement contributions', '#82E0AA', '📈', TRUE, TRUE, 1, NULL, 12),
('Gifts & Donations', 'Gifts for family/friends, charitable donations, tips', '#F1948A', '🎁', TRUE, TRUE, 1, NULL, 13),
('Business Expenses', 'Office supplies, business meals, professional services', '#D2B4DE', '💼', TRUE, TRUE, 1, NULL, 14),
('Taxes', 'Income tax, property tax, tax preparation fees', '#AED6F1', '📋', TRUE, TRUE, 1, NULL, 15),
('Pet Care', 'Pet food, veterinary bills, pet supplies, grooming', '#A9DFBF', '🐕', TRUE, TRUE, 1, NULL, 16),
('Subscriptions', 'Streaming services, software subscriptions, memberships', '#FADBD8', '📱', TRUE, TRUE, 1, NULL, 17),
('Banking & Finance', 'Bank fees, ATM fees, financial advisor fees, loan payments', '#D5DBDB', '🏦', TRUE, TRUE, 1, NULL, 18),
('Childcare', 'Daycare, babysitting, children activities, school supplies', '#FCF3CF', '👶', TRUE, TRUE, 1, NULL, 19),
('Other', 'Miscellaneous expenses that don\'t fit other categories', '#EAECEE', '📦', TRUE, TRUE, 1, NULL, 20);

-- Note: created_by is set to 1 (assuming the first user/admin user)
-- In a real application, you might want to create these categories for each new user
-- or have a system user (id=0) that owns system categories

-- Add some additional popular subcategories as regular categories
INSERT INTO categories (
    name, 
    description, 
    color, 
    icon, 
    is_active, 
    is_system, 
    created_by, 
    team_id, 
    sort_order
) VALUES 
-- Popular subcategories
('Groceries', 'Supermarket shopping, fresh produce, household items', '#FF8A80', '🛒', TRUE, TRUE, 1, NULL, 21),
('Coffee & Tea', 'Coffee shops, tea purchases, café visits', '#8D6E63', '☕', TRUE, TRUE, 1, NULL, 22),
('Fast Food', 'Quick service restaurants, drive-through meals', '#FF7043', '🍔', TRUE, TRUE, 1, NULL, 23),
('Gas & Fuel', 'Vehicle fuel, gas station purchases', '#607D8B', '⛽', TRUE, TRUE, 1, NULL, 24),
('Parking', 'Parking fees, parking meters, garage fees', '#78909C', '🅿️', TRUE, TRUE, 1, NULL, 25),
('Clothing', 'Apparel, shoes, accessories, fashion items', '#E1BEE7', '👕', TRUE, TRUE, 1, NULL, 26),
('Electronics', 'Gadgets, computers, phones, electronic accessories', '#90CAF9', '📱', TRUE, TRUE, 1, NULL, 27),
('Books & Media', 'Books, magazines, digital media, audiobooks', '#FFCC02', '📖', TRUE, TRUE, 1, NULL, 28),
('Pharmacy', 'Medications, health supplements, medical supplies', '#C8E6C9', '💊', TRUE, TRUE, 1, NULL, 29),
('Internet & Phone', 'Internet bills, mobile phone bills, data plans', '#B39DDB', '📶', TRUE, TRUE, 1, NULL, 30);

-- Create an "Uncategorized" category for expenses without a category
INSERT INTO categories (
    name, 
    description, 
    color, 
    icon, 
    is_active, 
    is_system, 
    created_by, 
    team_id, 
    sort_order
) VALUES 
('Uncategorized', 'Expenses that have not been categorized yet', '#9E9E9E', '❓', TRUE, TRUE, 1, NULL, 999);

-- Add some comments for documentation
-- Note: The created_by field is set to 1, which should be updated to match your system admin user ID
-- These system categories will be available to all users as templates
-- Users can create their own categories or customize these as needed
-- The sort_order helps maintain a logical order in the UI
-- Colors use hex codes that are visually distinct and pleasant
-- Icons use emojis for universal compatibility and visual appeal