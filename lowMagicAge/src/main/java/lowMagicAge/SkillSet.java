package lowMagicAge;

import java.util.function.Function;

public class SkillSet {

	public static class PChar {
		int INT;
		int WIS;
		int DEX;
		String name;
		private int baseSkillPointsPerLevel;
		
		public PChar(String name, int DEX, int INT, int WIS, int baseSkill) {
			this.name = name;
			this.DEX = DEX;
			this.INT = INT;
			this.WIS = WIS;
			this.baseSkillPointsPerLevel = baseSkill;
		}
		
	}
	
	public static class Skill {
		
		String name;
		private Function<PChar, Integer> baseRank;
		private int desiredCount;
		private int numChars;
		public Skill(String name,Function<PChar,Integer> baseRank, int desiredCount, int numChars) {
			this.name = name;
			this.baseRank = baseRank;
			this.desiredCount=desiredCount;
			this.numChars = numChars;
		}
		
		
	}
	
	
	
	public static void main(String[] args) {
		Skill[] theSkills = new Skill[] {
				new Skill("Heal",(x)->(x.INT-10)/2,10,3),
				new Skill("Listen",(x)->(x.WIS-10)/2,5,2),
				new Skill("Search",(x)->(x.INT-10)/2,20,2),
				new Skill("Disable Device,(x)->(x.
		}
		
		
	}
	
}
