# Smart Dining — upgraded source

## Stack
- Android Java Activities
- XML layouts / standard Android Views
- SQLite via `SQLiteOpenHelper`
- Gemini REST API for the optional AI assistant

## New features
- Food image URL field on Add Food and Edit Food; images appear in hotel menus, menu management, daily-special selection, food details, cart-cluster details, order history, and AI recommendation cards.
- Multiple daily specials per hotel/day. Reopen the Daily Specials screen to see current selections; tap a selected dish to remove it or an unselected dish to add it.
- Consistent in-app toolbar with back navigation on non-home screens; customer and hotel home screens have top-right logout.
- Floating AI button appears only on the customer home screen.
- AI uses available food/menu data, hotel names, prices, ratings, daily specials, local time, and the customer's recent orders. It supports budget-aware suggestions and can resolve multi-item orders into a selected or newly created cart cluster.
- Database schema upgrades from version 1 to version 2 by adding `food.image_url` without dropping existing app data.

## AI setup
1. Run the app and sign in as a customer.
2. Tap the round **✦ AI** button on the customer home screen.
3. Open the Google AI Studio API key page from the helper text, create a Gemini API key, paste it into the key field, and tap **Save**.
4. Ask for suggestions, for example: `My budget is 300 rs. Suggest dinner options.`
5. To order multiple items, use a clear request such as: `Order 2 chicken biryani and 1 paneer tikka from [hotel name].` The assistant shows the resolved items and estimated total, then lets you choose an existing cart cluster or create a new one before adding them.

The AI feature requires an internet connection and a valid Gemini API key. The key is stored in this app's local SharedPreferences; do not use a production secret in a classroom/demo app.

## Image links
Use direct HTTPS image URLs (links that return an image file, not a webpage). If a URL is empty, invalid, or unreachable, the app shows a neutral placeholder.

## Open in Android Studio
Open the extracted `SmartDining` folder. Android Studio will regenerate `local.properties` for your machine; that file is intentionally not included in the ZIP. Let Gradle sync finish before running.
