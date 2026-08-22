/* Flat, geometric decorative illustrations — simple shapes only (no
   traced/complex path data), Blinkit-style: bold yellow, confident
   rounded forms, a little playful motion. Placeholder art standing in
   for real product imagery, which doesn't exist until catalog-service
   (Stage 4) — these are honestly decorative, not product photography. */

export function DeliveryIllustration(props) {
  return (
    <svg viewBox="0 0 360 300" fill="none" {...props}>
      <circle cx="180" cy="150" r="150" fill="var(--brand-soft)" />
      <circle cx="270" cy="70" r="26" fill="var(--brand)" opacity="0.5" />
      <circle cx="60" cy="230" r="18" fill="var(--brand)" opacity="0.35" />

      {/* ground */}
      <line x1="40" y1="234" x2="320" y2="234" stroke="var(--line)" strokeWidth="3" strokeLinecap="round" />

      {/* motion lines */}
      <g stroke="var(--faint)" strokeWidth="4" strokeLinecap="round" opacity="0.6">
        <line x1="30" y1="150" x2="58" y2="150" />
        <line x1="24" y1="172" x2="60" y2="172" />
        <line x1="34" y1="194" x2="56" y2="194" />
      </g>

      {/* delivery box on back */}
      <rect x="205" y="140" width="52" height="46" rx="8" fill="var(--brand)" />
      <rect x="205" y="140" width="52" height="14" rx="7" fill="var(--brand-ink)" opacity="0.12" />
      <path d="M231 140v46" stroke="var(--brand-ink)" strokeWidth="2" opacity="0.35" />

      {/* scooter body */}
      <path
        d="M96 234c0-10 8-18 18-18h52l24-30h30c8 0 14 6 14 14v10h20"
        stroke="var(--ink)"
        strokeWidth="7"
        strokeLinecap="round"
        strokeLinejoin="round"
        fill="none"
      />
      <path d="M150 216h56" stroke="var(--ink)" strokeWidth="7" strokeLinecap="round" />

      {/* handlebar post */}
      <path d="M204 186v-34" stroke="var(--ink)" strokeWidth="7" strokeLinecap="round" />
      <path d="M188 152h34" stroke="var(--ink)" strokeWidth="7" strokeLinecap="round" />

      {/* wheels */}
      <circle cx="118" cy="234" r="22" fill="var(--surface)" stroke="var(--ink)" strokeWidth="7" />
      <circle cx="118" cy="234" r="5" fill="var(--ink)" />
      <circle cx="240" cy="234" r="22" fill="var(--surface)" stroke="var(--ink)" strokeWidth="7" />
      <circle cx="240" cy="234" r="5" fill="var(--ink)" />

      {/* rider */}
      <circle cx="196" cy="108" r="16" fill="var(--ink)" />
      <path
        d="M196 124c-14 0-24 14-24 30v18"
        stroke="var(--ink)"
        strokeWidth="9"
        strokeLinecap="round"
        fill="none"
      />
      <path d="M188 138l14 16" stroke="var(--ink)" strokeWidth="8" strokeLinecap="round" />
    </svg>
  );
}

export function GroceryBagIllustration(props) {
  return (
    <svg viewBox="0 0 320 280" fill="none" {...props}>
      <circle cx="160" cy="140" r="130" fill="var(--brand-soft)" />

      {/* bag */}
      <path
        d="M92 118h136l14 128a14 14 0 0 1-14 16H92a14 14 0 0 1-14-16z"
        fill="var(--brand)"
      />
      <path d="M92 118h136l4 34H88z" fill="var(--brand-ink)" opacity="0.1" />
      <path
        d="M112 118v-14a48 48 0 0 1 96 0v14"
        stroke="var(--ink)"
        strokeWidth="8"
        strokeLinecap="round"
        fill="none"
      />

      {/* produce peeking out */}
      <circle cx="130" cy="104" r="20" fill="#E5484D" />
      <path d="M130 84c4-8 12-10 16-8" stroke="#0C9950" strokeWidth="5" strokeLinecap="round" />

      <circle cx="176" cy="98" r="16" fill="#FFA23C" />

      <g>
        <circle cx="205" cy="112" r="8" fill="#7C5CD1" />
        <circle cx="216" cy="102" r="8" fill="#7C5CD1" />
        <circle cx="212" cy="118" r="8" fill="#7C5CD1" />
      </g>

      {/* soft accents */}
      <circle cx="270" cy="60" r="14" fill="var(--brand)" opacity="0.4" />
      <circle cx="46" cy="200" r="10" fill="var(--success)" opacity="0.3" />
    </svg>
  );
}

export function EmptyPinIllustration(props) {
  return (
    <svg viewBox="0 0 160 160" fill="none" {...props}>
      <circle cx="80" cy="80" r="70" fill="var(--surface-2)" />
      <circle cx="80" cy="80" r="46" stroke="var(--line)" strokeWidth="2" strokeDasharray="5 6" fill="none" />
      <path
        d="M80 40c-15 0-27 12-27 27 0 20 27 47 27 47s27-27 27-47c0-15-12-27-27-27Z"
        fill="var(--brand)"
      />
      <circle cx="80" cy="66" r="10" fill="var(--brand-ink)" opacity="0.85" />
    </svg>
  );
}

function CategoryGlyph({ children }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      {children}
    </svg>
  );
}

export function FruitGlyph() {
  return (
    <CategoryGlyph>
      <circle cx="12" cy="14" r="7" fill="#E5484D" stroke="none" />
      <path d="M12 7c1-3 4-3 5-2" stroke="#0C9950" />
    </CategoryGlyph>
  );
}

export function VeggieGlyph() {
  return (
    <CategoryGlyph>
      <path d="M12 4c4 3 6 9 3 15-5-1-8-6-6-11 1-2 2-3 3-4Z" fill="#FFA23C" stroke="none" />
      <path d="M13 5c1-2 3-2 4-1" stroke="#0C9950" />
    </CategoryGlyph>
  );
}

export function DairyGlyph() {
  return (
    <CategoryGlyph>
      <path d="M9 3h6l1 3-2 2v11a2 2 0 0 1-2 2h-0a2 2 0 0 1-2-2V8L8 6l1-3Z" fill="#5FB4E5" stroke="none" />
      <path d="M9 11h6" stroke="#fff" opacity="0.7" />
    </CategoryGlyph>
  );
}

export function BakeryGlyph() {
  return (
    <CategoryGlyph>
      <path d="M4 15c0-5 4-9 8-9s8 4 8 9-4 3-8 3-8 2-8-3Z" fill="#C98A4B" stroke="none" />
      <path d="M9 10c1-2 2-2 3 0M13 10c1-2 2-2 3 0" stroke="#7A5324" />
    </CategoryGlyph>
  );
}

export function SnackGlyph() {
  return (
    <CategoryGlyph>
      <path d="M7 4h10l1 5a8 6 0 0 1-12 0Z" fill="#7C5CD1" stroke="none" />
      <path d="M9 9h6M10 12h4" stroke="#fff" opacity="0.7" />
    </CategoryGlyph>
  );
}

export function CareGlyph() {
  return (
    <CategoryGlyph>
      <rect x="8" y="7" width="8" height="13" rx="2.5" fill="#34B08A" stroke="none" />
      <rect x="10" y="3" width="4" height="4" rx="1" fill="#34B08A" stroke="none" />
    </CategoryGlyph>
  );
}
