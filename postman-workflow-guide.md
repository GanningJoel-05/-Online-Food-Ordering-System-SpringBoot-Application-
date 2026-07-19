# Postman walkthrough - Food Ordering System

Base URL for every request below: `http://localhost:8080`

Make sure `docker-compose up -d` and `mvn spring-boot:run` are both running before you start.

---

## Step 1 - Register a restaurant owner

**POST** `/api/auth/register`

Headers: `Content-Type: application/json`

Body (raw JSON):
```json
{
  "email": "owner@pizzahub.com",
  "password": "owner123",
  "fullName": "Raj Kumar",
  "phoneNumber": "9876543210",
  "role": "RESTAURANT_OWNER"
}
```

Expected: `201 Created`, response body contains a `token`. **Copy this token** - call it `OWNER_TOKEN`.

---

## Step 2 - Register a customer

**POST** `/api/auth/register`

Body:
```json
{
  "email": "customer@gmail.com",
  "password": "customer123",
  "fullName": "Priya Sharma",
  "phoneNumber": "9123456780",
  "role": "CUSTOMER"
}
```

Expected: `201 Created`. **Copy this token** - call it `CUSTOMER_TOKEN`.

> Tip: In Postman, save both tokens as collection variables (`owner_token`, `customer_token`) so you can reference them as `{{owner_token}}` instead of pasting manually every time.

---

## Step 3 - Login (optional sanity check)

**POST** `/api/auth/login`

Body:
```json
{
  "email": "owner@pizzahub.com",
  "password": "owner123"
}
```

Expected: `200 OK`, same shape as register - a fresh token.

---

## Step 4 - Create a restaurant (as the owner)

**POST** `/api/restaurants`

Headers:
- `Content-Type: application/json`
- `Authorization: Bearer {{owner_token}}`

Body:
```json
{
  "name": "Pizza Hub",
  "description": "Wood-fired pizzas and Italian classics",
  "cuisineType": "Italian",
  "location": "Madurai"
}
```

Expected: `201 Created`, response includes an `id`. **Copy this** - call it `RESTAURANT_ID`.

> Try this without the `Authorization` header first - you should get `403 Forbidden`. That proves `@PreAuthorize` is actually blocking unauthenticated writes.

---

## Step 5 - Add menu items (as the owner)

**POST** `/api/restaurants/{{restaurant_id}}/menu-items`

Headers: `Authorization: Bearer {{owner_token}}`

Body (item 1):
```json
{
  "name": "Margherita Pizza",
  "description": "Classic tomato, mozzarella, basil",
  "price": 249.00,
  "category": "Main Course",
  "available": true
}
```

Repeat with a second item so your order has multiple line items:
```json
{
  "name": "Garlic Bread",
  "description": "Toasted with garlic butter",
  "price": 99.00,
  "category": "Starters",
  "available": true
}
```

Expected: `201 Created` each time. **Copy both `id` values** - call them `ITEM_1_ID` and `ITEM_2_ID`.

---

## Step 6 - Browse restaurants (as anyone — no token needed)

**GET** `/api/restaurants?search=italian&page=0&size=10`

No `Authorization` header required — this route is public. Expected: `200 OK`, a paginated list containing Pizza Hub.

**GET** `/api/restaurants/{{restaurant_id}}`

Expected: `200 OK`, full detail including the `menuItems` array — confirms both items you added are attached.

---

## Step 7 - Place an order (as the customer)

**POST** `/api/orders`

Headers:
- `Content-Type: application/json`
- `Authorization: Bearer {{customer_token}}`

Body:
```json
{
  "restaurantId": "{{restaurant_id}}",
  "items": [
    { "menuItemId": "{{item_1_id}}", "quantity": 2 },
    { "menuItemId": "{{item_2_id}}", "quantity": 1 }
  ],
  "deliveryAddress": "12 Anna Nagar, Madurai"
}
```

Expected: `201 Created`. Response includes `status: "PLACED"`, a computed `totalAmount` (2×249 + 1×99 = 597.00), and both order items. **Copy the order `id`** - call it `ORDER_ID`.

> Try this with the `owner_token` instead of `customer_token` - expect `403 Forbidden`, since `@PreAuthorize("hasRole('CUSTOMER')")` guards this endpoint.

---

## Step 8 - View the order

**GET** `/api/orders/{{order_id}}`

Headers: `Authorization: Bearer {{customer_token}}`

Expected: `200 OK`, `status: "PLACED"`.

**GET** `/api/orders/my-orders`

Headers: `Authorization: Bearer {{customer_token}}`

Expected: `200 OK`, a paginated list containing this order.

---

## Step 9 - Walk the order through the state machine (as the owner)

**PATCH** `/api/orders/{{order_id}}/status`

Headers:
- `Content-Type: application/json`
- `Authorization: Bearer {{owner_token}}`

Body:
```json
{ "newStatus": "CONFIRMED" }
```

Expected: `200 OK`, `status: "CONFIRMED"`.

Repeat this same request three more times, changing only the body each time, **in this exact order**:

```json
{ "newStatus": "PREPARING" }
```
```json
{ "newStatus": "OUT_FOR_DELIVERY" }
```
```json
{ "newStatus": "DELIVERED" }
```

Each should return `200 OK` with the updated status.

---

## Step 10 - Prove the state machine actually enforces rules

Now that the order is `DELIVERED`, try to move it backward:

**PATCH** `/api/orders/{{order_id}}/status`

Body:
```json
{ "newStatus": "PLACED" }
```

Expected: **`409 Conflict`**, with a response body like:
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Cannot move order from DELIVERED to PLACED"
}
```

This is the single most important thing to demo in an interview - it proves the transition map in `OrderService` is doing real enforcement, not just accepting any string.

---

## Step 11 - Test customer-side cancellation on a fresh order

Repeat **Step 7** to place a second order (`ORDER_ID_2`), leaving it in `PLACED` status. Then:

**PATCH** `/api/orders/{{order_id_2}}/cancel`

Headers: `Authorization: Bearer {{customer_token}}`

Expected: `200 OK`, `status: "CANCELLED"`.

Try cancelling it again — expect `409 Conflict`, since `CANCELLED` is a terminal state with no outgoing transitions.

---

## Step 12 - Test authorization boundaries

A few negative tests worth showing an interviewer, all of which should fail correctly:

| Request | Token used | Expected result |
|---|---|---|
| `PATCH /api/orders/{{order_id}}/status` | `customer_token` | `403` - customers can't drive the state machine forward, only cancel |
| `POST /api/restaurants` | `customer_token` | `403` - only `RESTAURANT_OWNER`/`ADMIN` can create restaurants |
| `PUT /api/restaurants/{{restaurant_id}}` | a *different* owner's token | `403` - ownership check in the service layer, not just role check |
| `GET /api/orders/{{order_id}}` | no `Authorization` header | `401` - this route requires authentication (unlike restaurant browsing) |

---

## Step 13 - Restaurant owner's incoming orders view

**GET** `/api/orders/restaurant/{{restaurant_id}}`

Headers: `Authorization: Bearer {{owner_token}}`

Expected: `200 OK`, paginated list of both orders placed against this restaurant, with their current statuses.

---

## Optional: exporting this as a Postman Collection

If you'd rather not click through manually every time:
1. In Postman, create a new Collection called "Food Ordering System"
2. Add a Collection-level variable `base_url` = `http://localhost:8080`
3. After Step 1 and Step 2, right-click each request → **Tests** tab → add:
   ```javascript
   pm.collectionVariables.set("owner_token", pm.response.json().token);
   ```
   (swap `owner_token` for `customer_token` on the customer registration request)
4. Now every subsequent request can just use `{{owner_token}}` / `{{customer_token}}` in its Authorization header, auto-populated — no manual copy-pasting between requests.

---

## Quick reference - every endpoint used above

| Step | Method | Endpoint |
|---|---|---|
| 1, 2 | POST | `/api/auth/register` |
| 3 | POST | `/api/auth/login` |
| 4 | POST | `/api/restaurants` |
| 5 | POST | `/api/restaurants/{id}/menu-items` |
| 6 | GET | `/api/restaurants`, `/api/restaurants/{id}` |
| 7 | POST | `/api/orders` |
| 8 | GET | `/api/orders/{id}`, `/api/orders/my-orders` |
| 9, 10 | PATCH | `/api/orders/{id}/status` |
| 11 | PATCH | `/api/orders/{id}/cancel` |
| 13 | GET | `/api/orders/restaurant/{id}` |
