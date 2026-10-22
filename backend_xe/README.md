# RentWheels Java application

The REST API and customer/admin web interfaces run on Java 17, Spring Boot, and
Vaadin. Customer pages and the admin dashboard are served from the same
application; Vaadin calls the existing Java API controllers directly.

## Create the MySQL database

1. Start the MySQL server in Navicat and connect to it.
2. Open `database/init.sql` in Navicat and run it. It creates the `xe` database
   and the tables needed by the API. The script uses `CREATE ... IF NOT EXISTS`;
   it does not drop or overwrite existing tables.
3. To add demo vehicles, open `database/sample-data.sql` in Navicat and run it.
   It inserts sample cars only when `thong_tin_xe` is empty. The image paths
   refer to the customer app's `public/assets/cars` files.
4. To add demo accounts, bookings, favorites, reviews, and messages, open
   `database/sample-related-data.sql` and run it after `sample-data.sql`.
   Demo account password: `Demo123!`. Admin: `admin.demo@rentwheels.local`;
   customers: `minhanh.demo@rentwheels.local` and `giabao.demo@rentwheels.local`.
   The demo password hash assumes the default `JWT_SECRET` value
   (`rentwheels_secret_key`). If you configured a different secret, register
   accounts through the app instead. These accounts and password are for local
   testing only; do not use them on a public deployment.
5. New databases start without user accounts. Register a user through the customer interface.
   To grant that account admin access, run the following in Navicat, replacing
   the email with the registered account:

   ```sql
   USE xe;
   UPDATE users SET role = 'admin' WHERE email = 'your-admin@example.com';
   ```

## Run the application

To start the customer and admin Java interfaces on separate ports from the
repository root, run this in a PowerShell terminal:

```powershell
powershell -ExecutionPolicy Bypass -File .\start-local.ps1
```

The customer interface runs on port `3000`; the admin interface runs on port
`3001` with an isolated session cookie and redirects customer routes to the
admin login. Admin accounts signing in through the customer login open the
admin interface in a new tab and are signed out of the customer session.
Regular customer accounts stay signed in, land on the customer home page, and
see personalized links to their profile and bookings. Add
`-NoBrowser` to start or check both applications without opening a browser tab.
Use `-CustomerPort` and `-AdminPort` to choose alternate ports if either
default port is already in use. The script reuses matching app instances and
reads the local backend `.env` file when present. Both interfaces share the same
MySQL database and API. The Maven Wrapper downloads Maven on first use. The old
React/Vite source and build configuration have been removed; only
`baitap1/public/assets` remains for the image files served by the Java app.

To keep the server logs in the current terminal and stop instances started by
that command with `Ctrl+C`, add `-Foreground`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\start-local.ps1 -Foreground
```

If the default ports are occupied, choose unused ports, for example
`-Foreground -CustomerPort 3200 -AdminPort 3201`.

The admin dashboard includes searchable vehicle management, booking review
(approve or reject pending requests), a message inbox with reply/read actions,
and live statistics with monthly booking, revenue, status, and vehicle-category
charts.

Install JDK 17 or 21, then run these commands from this directory.
Spring Boot reads these values from the process environment; the local startup
script imports the backend `.env` file when present.

```powershell
$env:DB_HOST = 'localhost'
$env:DB_PORT = '3306'
$env:DB_NAME = 'xe'
$env:DB_USER = 'root'
$env:DB_PASSWORD = ''
.\mvnw.cmd spring-boot:run
```

Set `DB_USER` and `DB_PASSWORD` to the MySQL account configured on your server.
For deployments, set `JWT_SECRET` to a private, stable value; it must remain
unchanged while existing accounts and tokens use this database.

Build a production-ready JAR with `.\mvnw.cmd -Pproduction package`; use JDK 21
for the Vaadin production bundle. The JAR is written to
`target/backend-xe-1.0.0.jar`. Customer routes, including `/vehicles`, are
served on port `3000`; `/admin/dashboard` is served on port `3001`. The REST
API remains under `/api` on both instances.

## API compatibility

The server retains the existing `/api/auth`, `/api/cars`, `/api/bookings`,
`/api/favorites`, `/api/reviews`, and `/api/messages` routes used by the
frontends. The MySQL schema and API use the existing `thong_tin_xe`,
`booking_requests`, and `users` table names.
