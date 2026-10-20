# Lampstand Web Auth Wireframes (projectLampStand)

Source: `app/auth/page.tsx` + `app/onboarding/page.tsx`.
Flow: Screen 1 → 2 → 3 → 4 → `/` dashboard.
Returning `hasCompletedOnboarding=true` → `/` directly.

---

## Screen 1 — `/auth` Step 1: Welcome

```
┌─────────────────────────┐
│ [logo 48px]             │
│                         │
│ Welcome to Lampstand!   │  24px bold, primary
│ Your formation journey  │  18px body
│ starts here.            │
│                         │
│ ┌─────────────────────┐ │
│ │ G Continue w/Google │ │  white, grey border, 56px
│ └─────────────────────┘ │
│                         │
│ [DEV ONLY box, amber]   │
│ ⚡ Dev Mode             │
│ [Dev Login one-click]   │
│                         │
│ (email input hidden —   │
│  only used for dev OTP) │
└─────────────────────────┘
```

---

## Screen 2 — `/auth` Step 2: Verification Code

```
┌─────────────────────────┐
│ Verification Code       │  24px bold
│ Enter the 4-digit code  │
│ we sent to {email}      │
│                         │
│  ___  ___  ___  ___     │  64px boxes, bottom-border
│   1    2    3    4       │  auto-submit on 4th digit
│                         │
│ [red error text]        │  Invalid or expired code
│                         │
│ Didn't receive a code?  │
│ Resend code / 60s timer │  countdown, then clickable
│                         │
│ ┌─────────────────────┐ │
│ │ Continue (disabled  │ │  disabled until 4 digits
│ │ until 4 filled)     │ │
│ └─────────────────────┘ │
│ [fullscreen Loader     │  while isLoading
│  overlay when sending] │
└─────────────────────────┘
```

---

## Screen 3 — `/onboarding` Step 1: Complete Your Profile

```
┌─────────────────────────┐
│ [logo]                  │
│ Complete Your Profile   │  24px bold
│ Please fill in the      │
│ information below to    │
│ create your account     │
│                         │
│ First Name              │
│ [Enter First Name    ]  │
│ Last Name               │
│ [Enter Last Name     ]  │
│ Phone Number (Optional) │
│ [flag NG|v][80123...]   │  country select + tel
│                         │
│ [red error]             │
│ ┌─────────────────────┐ │
│ │ Continue (disabled  │ │  until first+last filled
│ │ until names filled) │ │
│ └─────────────────────┘ │
└─────────────────────────┘
```

---

## Screen 4 — `/onboarding` Step 2: Season + Time + Contact

```
┌─────────────────────────┐
│ Where are you right now?│
│ Choose season...        │
│ (●) Struggling to trust │  selectable
│ ─── Coming Soon ───     │
│ [x] Feeling uncertain…  │  disabled, "soon"
│ [x] Living under… soon  │
│ [x] Waiting… soon       │
│ [x] Feeling distant soon│
│ [x] None… soon          │
│                         │
│ When daily session?     │
│ We'll send reminder…    │
│ (●) Early Morning       │  default selected
│     Before day begins   │
│ ( ) Afternoon / Midday  │
│ ( ) Evening / slow down │
│ ( ) Before Bed / quiet  │
│                         │
│ How keep in touch?      │
│ Only when it matters…   │
│ (●) Email ( )WA ( )Both │  default email
│ [phone field IF         │
│  WA or Both]            │
│                         │
│ ┌─────────────────────┐ │
│ │ Start My Journey    │ │  disabled until season+
│ │ (disabled if WA &   │ │  time+contact (+phone)
│ │  no phone)          │ │
│ └─────────────────────┘ │
└─────────────────────────┘
```

Legend: `●` = selected radio (blue circle + check), `( )` = unselected.
