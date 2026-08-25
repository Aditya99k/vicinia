import {
  BakeryGlyph,
  BeverageGlyph,
  CareGlyph,
  DairyGlyph,
  FrozenGlyph,
  FruitGlyph,
  GrainGlyph,
  HouseholdGlyph,
  SnackGlyph,
  VeggieGlyph,
} from './Illustrations';

/** Maps a real catalog category name (see docker/mongo seed data) to its glyph — falls back to a grain glyph (the closest thing to a generic grocery icon) for anything unrecognized, so a new admin-created category never renders blank. */
const MATCHERS = [
  [/fruit/i, FruitGlyph],
  [/veget/i, VeggieGlyph],
  [/dairy|egg/i, DairyGlyph],
  [/bakery/i, BakeryGlyph],
  [/snack/i, SnackGlyph],
  [/personal care|care/i, CareGlyph],
  [/beverage/i, BeverageGlyph],
  [/frozen/i, FrozenGlyph],
  [/household/i, HouseholdGlyph],
  [/atta|rice|dal|grain/i, GrainGlyph],
];

export default function CategoryGlyphFor({ name }) {
  const Glyph = MATCHERS.find(([re]) => re.test(name || ''))?.[1] || GrainGlyph;
  return <Glyph />;
}
