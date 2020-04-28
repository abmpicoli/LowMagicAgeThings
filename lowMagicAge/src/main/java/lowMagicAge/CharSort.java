package lowMagicAge;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.print.DocFlavor.CHAR_ARRAY;

public class CharSort {
	static enum ATTRIBUTE {
		Str, Dex, Con, Int, Wis, Cha
	};

	private static class Race {
		final String name;
		final String weaponProficiency;
		final boolean receivesExtraAbility;
		final Map<ATTRIBUTE, Integer> attributes = new HashMap<>();
		final ChanceMap<CharClass> charClassChance = new ChanceMap<>();

		public Race(String name, String weaponProficiency, boolean receivesExtraHability, Consumer<Race> initializer) {
			this.name = name;
			this.weaponProficiency = weaponProficiency;
			this.receivesExtraAbility = receivesExtraHability;
			initializer.accept(this);
		}
	}

	private static class CharClass {
		final String name;
		final boolean useWeaponProficiencies;
		boolean isMagicUser = false;
		public ChanceMap<Feat> featChances = new ChanceMap<>();

		public CharClass(String name, boolean useWeaponProficiencies, Consumer<CharClass> initializer) {
			this.name = name;
			this.useWeaponProficiencies = useWeaponProficiencies;
			initializer.accept(this);
			charClassPerName.put(name, this);
		}

		public Map<ATTRIBUTE, Double> weights = new HashMap<>();

		@Override
		public String toString() {
			return name;
		}

	}

	private static final Race[] RACE = new Race[] { new Race("Human", null, true, (X) -> {
		X.attributes.put(ATTRIBUTE.Str, 8);
		X.attributes.put(ATTRIBUTE.Dex, 8);
		X.attributes.put(ATTRIBUTE.Con, 8);
		X.attributes.put(ATTRIBUTE.Int, 8);
		X.attributes.put(ATTRIBUTE.Wis, 8);
		X.attributes.put(ATTRIBUTE.Cha, 8);
		HUMAN = X;
	}), new Race("Dwarf", "Hammer", false, (X) -> {
		X.attributes.put(ATTRIBUTE.Str, 8);
		X.attributes.put(ATTRIBUTE.Dex, 8);
		X.attributes.put(ATTRIBUTE.Con, 10);
		X.attributes.put(ATTRIBUTE.Int, 8);
		X.attributes.put(ATTRIBUTE.Wis, 10);
		X.attributes.put(ATTRIBUTE.Cha, 6);
		DWARF = X;
	}), new Race("Elf", "Bow", false, (X) -> {
		X.attributes.put(ATTRIBUTE.Str, 8);
		X.attributes.put(ATTRIBUTE.Dex, 10);
		X.attributes.put(ATTRIBUTE.Con, 6);
		X.attributes.put(ATTRIBUTE.Int, 10);
		X.attributes.put(ATTRIBUTE.Wis, 8);
		X.attributes.put(ATTRIBUTE.Cha, 8);
		ELF = X;
	}), new Race("Gnome", "Crossbow", false, (X) -> {
		X.attributes.put(ATTRIBUTE.Str, 6);
		X.attributes.put(ATTRIBUTE.Dex, 8);
		X.attributes.put(ATTRIBUTE.Con, 8);
		X.attributes.put(ATTRIBUTE.Int, 10);
		X.attributes.put(ATTRIBUTE.Wis, 8);
		X.attributes.put(ATTRIBUTE.Cha, 10);
		GNOME = X;
	}), new Race("Half-elf", null, true, (X) -> {
		X.attributes.put(ATTRIBUTE.Str, 8);
		X.attributes.put(ATTRIBUTE.Dex, 8);
		X.attributes.put(ATTRIBUTE.Con, 8);
		X.attributes.put(ATTRIBUTE.Int, 8);
		X.attributes.put(ATTRIBUTE.Wis, 8);
		X.attributes.put(ATTRIBUTE.Cha, 8);
		HALFELF = X;
	}), new Race("Half-orc", "Axe", false, (X) -> {
		X.attributes.put(ATTRIBUTE.Str, 10);
		X.attributes.put(ATTRIBUTE.Dex, 8);
		X.attributes.put(ATTRIBUTE.Con, 10);
		X.attributes.put(ATTRIBUTE.Int, 6);
		X.attributes.put(ATTRIBUTE.Wis, 8);
		X.attributes.put(ATTRIBUTE.Cha, 6);
		HALFORC = X;
	}), new Race("Halfling", "Thrown", false, (X) -> {
		X.attributes.put(ATTRIBUTE.Str, 6);
		X.attributes.put(ATTRIBUTE.Dex, 10);
		X.attributes.put(ATTRIBUTE.Con, 8);
		X.attributes.put(ATTRIBUTE.Int, 8);
		X.attributes.put(ATTRIBUTE.Wis, 8);
		X.attributes.put(ATTRIBUTE.Cha, 10);
		HALFLING = X;
	}) };
	private static Race HUMAN;
	private static Race DWARF;
	private static Race ELF;
	private static Race GNOME;
	private static Race HALFELF;
	private static Race HALFORC;
	private static Race HALFLING;
	private static final String[] PROFICIENCIES = new String[] { "Sword", "Axe", "Hammer", "Flail", "Spear", "Bow",
			"Crossbow", "Thrown" };
	private static final Map<String, CharClass> charClassPerName = new HashMap<>();

	private static final CharClass[] CHARCLASS = new CharClass[] { new CharClass("Fighter", true, (X) -> {
		X.weights.put(ATTRIBUTE.Str, 3.0);
		X.weights.put(ATTRIBUTE.Dex, 2.0);
		X.weights.put(ATTRIBUTE.Con, 2.0);
		X.weights.put(ATTRIBUTE.Int, 1.0);
		X.weights.put(ATTRIBUTE.Wis, 1.0);
		X.weights.put(ATTRIBUTE.Cha, 1.0);
		HUMAN.charClassChance.add(X, 1.0);
		ELF.charClassChance.add(X, 1.0);
		DWARF.charClassChance.add(X, 3.0);
		GNOME.charClassChance.add(X, 0.66);
		HALFELF.charClassChance.add(X, 1.0);
		HALFORC.charClassChance.add(X, 1.5);
		HALFLING.charClassChance.add(X, 0.66);

	}), new CharClass("Cleric", false, (X) -> {
		X.weights.put(ATTRIBUTE.Str, 1.5);
		X.weights.put(ATTRIBUTE.Dex, 1.0);
		X.weights.put(ATTRIBUTE.Con, 2.0);
		X.weights.put(ATTRIBUTE.Int, 2.0);
		X.weights.put(ATTRIBUTE.Wis, 3.0);
		X.weights.put(ATTRIBUTE.Cha, 2.0);
		X.isMagicUser = true;
		HUMAN.charClassChance.add(X, 1.0);
		ELF.charClassChance.add(X, 1.0);
		DWARF.charClassChance.add(X, 1.1);
		GNOME.charClassChance.add(X, 1.05);
		HALFELF.charClassChance.add(X, 1.0);
		HALFORC.charClassChance.add(X, 0.9);
		HALFLING.charClassChance.add(X, 1.05);
	}), new CharClass("Wizard", false, (X) -> {
		X.weights.put(ATTRIBUTE.Str, 1.0);
		X.weights.put(ATTRIBUTE.Dex, 1.0);
		X.weights.put(ATTRIBUTE.Con, 1.0);
		X.weights.put(ATTRIBUTE.Int, 3.0);
		X.weights.put(ATTRIBUTE.Wis, 2.0);
		X.weights.put(ATTRIBUTE.Cha, 2.0);
		X.isMagicUser = true;
		HUMAN.charClassChance.add(X, 1.0);
		ELF.charClassChance.add(X, 3.0);
		DWARF.charClassChance.add(X, 1.0);
		GNOME.charClassChance.add(X, 3.0);
		HALFELF.charClassChance.add(X, 1.0);
		HALFORC.charClassChance.add(X, 0.66);
		HALFLING.charClassChance.add(X, 1.0);
	}), new CharClass("Rogue", false, (X) -> {
		X.weights.put(ATTRIBUTE.Str, 2.0);
		X.weights.put(ATTRIBUTE.Dex, 3.0);
		X.weights.put(ATTRIBUTE.Con, 2.0);
		X.weights.put(ATTRIBUTE.Int, 3.0);
		X.weights.put(ATTRIBUTE.Wis, 2.0);
		X.weights.put(ATTRIBUTE.Cha, 2.0);
		HUMAN.charClassChance.add(X, 1.0);
		ELF.charClassChance.add(X, 1.1);
		DWARF.charClassChance.add(X, 0.9);
		GNOME.charClassChance.add(X, 1.1);
		HALFELF.charClassChance.add(X, 1.0);
		HALFORC.charClassChance.add(X, 0.66);
		HALFLING.charClassChance.add(X, 3.0);
	}), new CharClass("Barbarian", true, (X) -> {
		X.weights.put(ATTRIBUTE.Str, 3.0);
		X.weights.put(ATTRIBUTE.Dex, 3.0);
		X.weights.put(ATTRIBUTE.Con, 3.0);
		X.weights.put(ATTRIBUTE.Int, 2.0);
		X.weights.put(ATTRIBUTE.Wis, 2.0);
		X.weights.put(ATTRIBUTE.Cha, 1.0);
		HUMAN.charClassChance.add(X, 1.0);
		ELF.charClassChance.add(X, 0.9);
		DWARF.charClassChance.add(X, 1.1);
		GNOME.charClassChance.add(X, 0.66);
		HALFELF.charClassChance.add(X, 1.0);
		HALFORC.charClassChance.add(X, 3.0);
		HALFLING.charClassChance.add(X, 0.66);

	}) };

	private static final CharClass FIGHTER = charClassPerName.get("Fighter");
	private static final CharClass CLERIC = charClassPerName.get("Cleric");
	private static final CharClass WIZARD = charClassPerName.get("Wizard");
	private static final CharClass ROGUE = charClassPerName.get("Rogue");
	private static final CharClass BARBARIAN = charClassPerName.get("Barbarian");

	static class Feat {

		private String name;

		public Feat(String name, Consumer<Feat> initializer) {
			this.name = name;
			initializer.accept(this);

		}
		@Override
		public String toString() {
			return name;
		}
	}

	public static final Feat[] FEATS = new Feat[] { new Feat("Imp.Initiative", (X) -> {
		for (CharClass y : CHARCLASS) {
			y.featChances.put(X, 5);
		}
	}), new Feat("Quick Draw", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 20.0);
			} else {
				y.featChances.put(X, 50.0);
			}
		}
	}), new Feat("Toughness", (X) -> {
		FIGHTER.featChances.put(X, 4.0);
		CLERIC.featChances.put(X, 2.0);
		WIZARD.featChances.put(X, 1.0);
		ROGUE.featChances.put(X, 2.0);
		BARBARIAN.featChances.put(X, 5.0);
	}), new Feat("Great Fortitude", (X) -> {
		FIGHTER.featChances.put(X, 1.0);
		CLERIC.featChances.put(X, 1.0);
		WIZARD.featChances.put(X, 1.0);
		ROGUE.featChances.put(X, 1.0);
		BARBARIAN.featChances.put(X, 1.0);
	}), new Feat("Lightning Reflexes", (X) -> {
		FIGHTER.featChances.put(X, 4.0);
		CLERIC.featChances.put(X, 2.0);
		WIZARD.featChances.put(X, 1.0);
		ROGUE.featChances.put(X, 2.0);
		BARBARIAN.featChances.put(X, 5.0);
	}), new Feat("Iron Will", (X) -> {
		for (CharClass y : CHARCLASS) {
			y.featChances.put(X, 2.0);
		}
	}), new Feat("Imp. Defenses", (X) -> {
		for (CharClass y : CHARCLASS) {
			y.featChances.put(X, 2.0);
		}
	}), new Feat("Dodge", (X) -> {
		for (CharClass y : CHARCLASS) {
			y.featChances.put(X, 2.0);
		}
	}), new Feat("Mobility", (X) -> {
		for (CharClass y : CHARCLASS) {
			y.featChances.put(X, 2.0);
		}
	}), new Feat("Weapon Prof.: Sword", (X) -> {
		for (CharClass y : CHARCLASS) {
			y.featChances.put(X, 1.0);
		}
	}), new Feat("Weapon Prof.: Axe", (X) -> {
		for (CharClass y : CHARCLASS) {
			y.featChances.put(X, 1.0);
		}
	}), new Feat("Weapon Prof.: Hammer", (X) -> {
		for (CharClass y : CHARCLASS) {
			y.featChances.put(X, 1.0);
		}
	}), new Feat("Weapon Prof.: Flail", (X) -> {
		for (CharClass y : CHARCLASS) {
			y.featChances.put(X, 1.0);
		}
	}), new Feat("Weapon Prof.: Spear", (X) -> {
		for (CharClass y : CHARCLASS) {
			y.featChances.put(X, 1.0);
		}
	}), new Feat("Weapon Prof.: Bow", (X) -> {
		for (CharClass y : CHARCLASS) {
			y.featChances.put(X, 1.0);
		}
	}), new Feat("Weapon Prof.: Crossbow", (X) -> {
		for (CharClass y : CHARCLASS) {
			y.featChances.put(X, 1.0);
		}
	}), new Feat("Weapon Prof.: Thrown", (X) -> {
		for (CharClass y : CHARCLASS) {
			y.featChances.put(X, 1.0);
		}
	}), new Feat("Weapon Prof.: Unarmed", (X) -> {
		for (CharClass y : CHARCLASS) {
			y.featChances.put(X, 1.0);
		}
	}), new Feat("Weapon Focus", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 2.0);
			}
		}
	}), new Feat("Weapon Specialization", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Imp. Critical", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Gtr. Critical", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Power Critical", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Bleeding Critical", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Crippling Critical", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Shield Prof.", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Tower Shield Prof.", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Shield Focus", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Light Armor Prof.", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 2.0);
			}
		}
	}), new Feat("Medium Armor Prof.", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 2.0);
			}
		}
	}), new Feat("Heavy Armor Prof.", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 2.0);
			}
		}
	}), new Feat("Power Attack", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Cleave", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Great Cleave", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Imp.Cleave", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Sweeping Strikes", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Imp. Sweeping Strikes", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Vital Strike", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Imp. Charge", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Gtr. Charge", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Combat Reflexes", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Weapon Finesse", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Improved Feint", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("TW Fighting", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Imp. TW Fighting", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Gtr. TW Fighting", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("TW Defence", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Imp. TW Defence", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Double Slice", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("TW Rend", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("TH Fighting", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Knockdown", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("TH Focus", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Point-Blank Shot", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 2.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Precise Shot", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 2.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Imp. Precise Shot", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 2.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Rapid Shot", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 2.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Imp. Rapid Shot", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 2.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Manyshot", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 2.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Imp. Manyshot", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 2.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Deadly Aim", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 2.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Crossbow Sniper", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 2.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Zen Archery", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 2.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Combat Maneuvers Familiarity", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Trip", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Imp. Trip", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Gtr. Trip", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Grapple", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Imp. Grapple", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Gtr. Grapple", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Bull Rush", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Imp. Bull Rush", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Gtr. Bull Rush", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Drag", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Imp. Drag", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Gtr. Drag", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Overrun", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Imp.Overrun", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Gtr.Overrun", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Agile Maneuvers", (X) -> {
		for (CharClass y : CHARCLASS) {
			if (y.isMagicUser) {
				y.featChances.put(X, 1.0);
			} else {
				y.featChances.put(X, 4.0);
			}
		}
	}), new Feat("Imp. Unarmed Attack", (X) -> {
		for (CharClass y : CHARCLASS) {
			y.featChances.put(X, 1.0);
		}
	}), new Feat("Spell Focus: Conjuration", (X) -> {
		CLERIC.featChances.put(X, 10.0);
		WIZARD.featChances.put(X, 10.0);
	}), new Feat("Augment Summoning", (X) -> {
		CLERIC.featChances.put(X, 10.0);
		WIZARD.featChances.put(X, 10.0);
	}), new Feat("Spell Focus: Elemental", (X) -> {
		CLERIC.featChances.put(X, 10.0);
		WIZARD.featChances.put(X, 10.0);
	}), new Feat("Elemental Specialization", (X) -> {
		CLERIC.featChances.put(X, 10.0);
		WIZARD.featChances.put(X, 10.0);
	}), new Feat("Spell Focus: Control", (X) -> {
		CLERIC.featChances.put(X, 10.0);
		WIZARD.featChances.put(X, 10.0);
	}), new Feat("Spell Focus: Necromancy", (X) -> {
		CLERIC.featChances.put(X, 10.0);
		WIZARD.featChances.put(X, 10.0);
	}), new Feat("Combat Casting", (X) -> {
		CLERIC.featChances.put(X, 10.0);
		WIZARD.featChances.put(X, 10.0);
	}), new Feat("Spell Penetration", (X) -> {
		CLERIC.featChances.put(X, 10.0);
		WIZARD.featChances.put(X, 10.0);
	}), new Feat("Gtr. Spell Penetration", (X) -> {
		CLERIC.featChances.put(X, 10.0);
		WIZARD.featChances.put(X, 10.0);
	}), new Feat("Imp. Turning", (X) -> {
		CLERIC.featChances.put(X, 10.0);
	}), new Feat("Gtr. Turning", (X) -> {
		CLERIC.featChances.put(X, 10.0);
	}), new Feat("Extra Turning", (X) -> {
		CLERIC.featChances.put(X, 10.0);
	}) };

	public static int[] costs = new int[] { 0, 1, 1, 1, 1, 1, 1, 2, 2, 3, 3 };


	public static void main(String[] args) throws Exception {
		ChanceMap<CharClass> classChance = new ChanceMap<>(Arrays.asList(CHARCLASS),6.0/5);
		ChanceMap<String> remainingProficiencies = new ChanceMap<>(Arrays.asList(PROFICIENCIES),6.0/5);
		Random sr = new Random(SecureRandom.getInstanceStrong().nextLong());
		TreeSet<String> soFar = new TreeSet<String>();
		for (int charIndex = 0; charIndex < 6; ++charIndex) {
			StringWriter sw = new StringWriter();
			PrintWriter out = new PrintWriter(sw);
			CharClass primaryClass = classChance.randomSubtraction(sr, 1.0);
			System.out.println("Class chosen: " + primaryClass + " remainingClasses:" + classChance);
			ChanceMap<CharClass> theClassWeights = new ChanceMap<>();
			theClassWeights.add(primaryClass,1.0);
			ChanceMap<Race> raceChance = new ChanceMap<>();
			for(Race x: RACE) {
				raceChance.add(x, x.charClassChance.chance(primaryClass));
			}
			Race race = raceChance.random(sr);
			while(sr.nextInt(6)==0) {
				CharClass extraClass = race.charClassChance.random(sr); 
				double weight = sr.nextDouble();
				theClassWeights.add(extraClass,weight);
				classChance.add(extraClass, -weight);
				System.out.println("Secondary class chosen: " + extraClass + "("+weight+") remainingClasses:" + classChance);
			}
			
			out.println("RACE:" + race.name);
			out.print("CLASS: "); 
			for(Entry<CharClass, Double> x: theClassWeights.entrySet()) {
				out.printf("%s (%5.2f) ;",x.getKey().name,x.getValue());
			}
			out.println();
			ATTRIBUTE extraAttribute = null;
			if (race.receivesExtraAbility) {
				ChanceMap<ATTRIBUTE> extraAbilityChance = new ChanceMap<>();
				for(Entry<CharClass, Double> x: theClassWeights.entrySet()) {
					for(ATTRIBUTE a : Arrays.asList(ATTRIBUTE.values())) {
						double value = x.getKey().weights.get(a) * x.getValue();
						extraAbilityChance.add(a, value);
					}
				}
				System.out.println("ability chance: " + extraAbilityChance);
				extraAttribute = extraAbilityChance.random(sr);
				out.println("MAIN ATTR: " + extraAttribute );
			} else {
				out.println("#");
			}
			LinkedHashSet<String> proficiencies = new LinkedHashSet<String>();
			for(Entry<CharClass, Double> x: theClassWeights.entrySet()) {
				 if(CLERIC == x.getKey()) {
					 proficiencies.add("Hammer");
					 remainingProficiencies.add("Hammer", -x.getValue());
				 }
				 if(WIZARD == x.getKey()) {
					 proficiencies.add("Crossbow");
					 remainingProficiencies.add("Crossbow", -x.getValue());
				 }
			}
			if (race.weaponProficiency != null) {
				proficiencies.add(race.weaponProficiency);
				remainingProficiencies.add(race.weaponProficiency, -1.0);
			}
			
			
			if (primaryClass.useWeaponProficiencies) {

				int numProficiencies = proficiencies.size() + 2;
				
				while (proficiencies.size() < numProficiencies) {
					proficiencies.add(remainingProficiencies.randomSubtraction(sr,1.0));
				}
				
			} 
			if(proficiencies.size() > 0) {
				out.println("PROFICIENCIES: " + proficiencies);
			}
			else {
				out.println("--no proficiencies--");
			}
			System.out.println("Proficiencies chosen:"  + proficiencies);
			System.out.println("Remaining proficiencies:" + remainingProficiencies);

			out.println("#: " + (charIndex + 1));
			LinkedHashMap<ATTRIBUTE, AtomicInteger> attributes = new LinkedHashMap<>();
			for (ATTRIBUTE s : ATTRIBUTE.values()) {
				attributes.put(s, new AtomicInteger());
			}
			
			ChanceMap<ATTRIBUTE> attributeChance = new ChanceMap<>();
			for(Entry<CharClass, Double> x: theClassWeights.entrySet()) {
				attributeChance.addAll(x.getKey().weights, x.getValue());
			}
			int points = 32;
			while (points > 0) {
				ATTRIBUTE theAttr = attributeChance.random(sr);
				AtomicInteger current = attributes.get(theAttr);
				if (current.get() + 1 < costs.length) {
					int pointsToRemove = costs[current.get() + 1];
					if (pointsToRemove <= points) {
						points -= pointsToRemove;
						current.incrementAndGet();
					}
				}

			}
			out.println("Attributes:STR,DEX,CON,INT,WIS,CHA");
			for (ATTRIBUTE x : ATTRIBUTE.values()) {
				int atrResult = ((extraAttribute == x ? 2 : 0) + race.attributes.get(x) + attributes.get(x).get());
				int atrResult2 = atrResult + sr.nextInt(10);
				out.printf("%02d - %02d\n",atrResult,atrResult2);
			}
			out.println("Feat goals: ");
			List<String> feats = new ArrayList<>();
			ChanceMap<Feat> featChances = new ChanceMap<>();
			for(Entry<CharClass, Double> x : theClassWeights.entrySet()) {
				featChances.addAll(x.getKey().featChances, x.getValue());
			}
			while (feats.size() < 80) {
				Feat feat = featChances.random(sr);
				String theFeat = feat.name;
				feats.add(theFeat);
				featChances.add(feat, -featChances.chance(feat)*0.25);
			}
			for (String feat : feats) {
				out.println("  >" + feat);
			}
			soFar.add(sw.toString());
		}
		for (String s : soFar) {

			System.out.println(s);
		}

	}

}
