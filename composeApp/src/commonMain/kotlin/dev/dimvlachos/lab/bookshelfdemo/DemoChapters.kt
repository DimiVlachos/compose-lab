package dev.dimvlachos.lab.bookshelfdemo

/** A chapter of a demo book: its title and its paragraphs. */
internal class DemoChapter(val title: String, val paragraphs: List<String>) {
    /** Each paragraph split into its words, as the shelf's pages are set from. */
    val words: List<List<String>> = paragraphs.map { it.split(' ') }
}

/**
 * The text inside the books that are being read: three with a chapter of their own, the rest
 * sharing one. All of it is written for this demo.
 */
internal object DemoChapters {
    val CottonExchange =
        DemoChapter(
            "The Cotton Exchange",
            listOf(
                "The tide came up the channel before dawn, slow and brown, and by six o'clock the bales were already stacked three high along the Strand. Nell Avery walked the length of them with her ledger open against her arm, counting marks, while the stevedores sang under their breath and the gulls quarreled over a split sack of rice.",
                "Her father had kept the books for Whitcomb & Gray for twenty-two years, and for nineteen of those years he had kept them alone. Then his eyes began to fail, and he brought Nell down to the office one January morning, sat her at the high desk by the window, and told the clerks she would be reading the figures to him from now on. Nobody believed she would last the spring.",
                "That had been four years ago. Now the clerks brought their tallies to her first.",
                "\"Eleven hundred and forty from the Brazos boats,\" said Mr. Pruitt, laying a slip of paper on her desk. \"And the Liverpool steamer wants an answer by noon.\"",
                "\"The Liverpool steamer can want,\" Nell said, without looking up. \"Tell them the Sea Lark docks at ten, and nobody signs anything for Liverpool until we have seen what she carries.\"",
                "Pruitt hesitated in the doorway, his hat turning in his hands. He was a careful man who had been passed over twice and had decided, on the whole, to be pleasant about it. \"Mr. Gray asked whether your father would be in today.\"",
                "\"My father is resting.\" She dipped her pen. \"Mr. Gray may ask me.\"",
                "Outside, the street had filled with drays and mule carts and men in shirtsleeves shouting prices at one another across the mud. The Exchange bell would ring at nine, and until then every rumor in Galveston passed along the Strand at the speed of a running boy. Rain upriver. Rain in Waco. A fire in a warehouse at Houston, or a strike in New Orleans, or no fire and no strike at all, only a man who wanted the price to move.",
                "Nell had learned to listen to all of it and believe almost none. Her father had taught her that, too. The figures do not lie, he used to say, but the men who bring them to you very often do.",
                "At half past eight a boy came running with a telegram, his cap pushed back and his face red from the heat. She tore it open, read it twice, and set it face down on the blotter.",
                "The Sea Lark had not reached the bar. She had lost her rudder off Matagorda and was being towed in, slowly, by a tug that would not see the channel before evening. Whatever she carried would come too late for the morning's trading, and too late for Liverpool.",
                "Nell sat very still. Across the room the clock ticked over the clerks' bent heads, and somewhere below a crate slid off a cart and burst on the planks, and a man swore at length about it.",
                "There was a way through, she thought. There was nearly always a way through, if you were willing to look at the columns long enough and did not mind what anyone said about you afterward. She pulled the big ledger toward her and turned back three weeks, to the shipments nobody had wanted, the bales from the little upriver gins that came in short and damp and were sold off cheap to whoever would take them.",
                "Short and damp, she thought. But not so damp as all that.",
                "She ran her finger down the page. Forty bales from Hempstead, held in the long shed since the first of the month. Sixty from a farmer named Odom who had refused the price and refused it again, and was still refusing it, as far as she knew, from a boardinghouse on Twenty-third Street.",
                "When the bell rang at nine, she was already on her feet with her bonnet in her hand.",
                "\"Mr. Pruitt,\" she said, \"I need the key to the long shed, and I need you to find Mr. Odom before Mr. Gray finds me.\"",
                "Pruitt looked at her for a long moment. Then, for the first time in four years, he smiled at her as though he meant it, and reached for his coat.",
            ),
        )

    val FirstSnow =
        DemoChapter(
            "First Snow",
            listOf(
                "The snow came to Marsh Hollow on the ninth of November, a week before anyone had expected it, and it came quietly, the way it always did, so that Ada Pell did not notice it until she went out to the woodpile and found the path already gone.",
                "She stood on the porch step with the empty basket on her hip and looked at the valley. The maples along the brook had dropped the last of their leaves the night before, and now the snow lay on the bare branches in thin white lines, like something drawn there in pencil. The road down to the village was a pale ribbon between the stone walls. Nothing moved on it.",
                "\"Well,\" she said aloud, to no one, \"there's that.\"",
                "Her brother Walt had gone down to Montpelier on Tuesday to see about the bank, and he had promised to be back by Saturday. Saturday was tomorrow. If the snow kept on the way it was going, he would be lucky to get as far as the covered bridge, and luckier still to get his old Ford up the hill past the Kimball place, where the road turned sharp and the ditch ran deep on the downhill side.",
                "She filled the basket with split birch and carried it in, and filled it again, and again, until the box beside the stove was heaped and her arms ached in the familiar way. Then she put the kettle on and stood at the kitchen window with her hands around a cup of tea she did not much want, watching the flakes come down out of a sky the color of an old nickel.",
                "There had been a winter, when she was nine, when the snow came early like this and did not stop. Her mother had kept a candle in this same window every night for two weeks while their father was snowed in at the lumber camp on the far side of the ridge. Ada remembered waking in the dark and seeing the little flame reflected in the glass, doubled, so that it looked as though there were two candles, one inside and one out in the snow.",
                "She had asked her mother, once, what the second candle was for.",
                "\"That one's for him,\" her mother had said. \"So he knows we've got one burning on his side, too.\"",
                "In the afternoon the wind came up and the snow began to drift against the north side of the barn. Ada went out in Walt's big coat and fed the two cows and the old horse, Major, who looked at her with great patience, as though he had seen a great many first snows and did not expect much from this one. She broke the skin of ice on the trough with the heel of her boot and stood a while in the warm, close dark of the barn, listening to the animals chew.",
                "By four o'clock it was nearly night. She lit the lamps and banked the stove and set two places at the table, though she knew it was foolish, and then took one of them away again, and then, after a while, put it back.",
                "At half past five she heard it: a motor laboring somewhere below, the sound coming and going on the wind. She went to the window and wiped the glass with her sleeve. Far down the hill, where the road came out of the trees, two small yellow lights were moving very slowly, stopping, moving again.",
                "Ada watched them for a long time. Then she went to the cupboard, took out the candle stub her mother had kept in the same drawer for forty years, lit it, and set it on the sill.",
            ),
        )

    val LastCall =
        DemoChapter(
            "Last Call",
            listOf(
                "The Blue Plate Diner stood on the corner of Halsted and Twenty-sixth, and at two in the morning it was the brightest thing for a mile in any direction. Inside, under the long humming lights, Rosa Delgado wiped down the counter for the fourth time and watched the rain run in crooked lines down the front window.",
                "There were three customers. A night-shift nurse from Mercy, still in her scrubs, working through a slice of lemon pie with the slow concentration of someone too tired to taste it. A cab driver named Lenny who came in every night at one-thirty and ordered the same thing, eggs over easy and a short stack, and never once finished the short stack. And a young man in a wet gray coat at the end of the counter, who had come in at midnight, ordered coffee, and had not said a word since.",
                "Rosa had refilled his cup five times. He thanked her each time with a small nod, as if she were doing him a favor he could not quite afford.",
                "\"You think it's going to let up?\" Lenny asked, to the room in general.",
                "\"It's Chicago,\" said the nurse, without raising her eyes. \"It's going to do whatever it wants.\"",
                "Behind the pass, Mr. Kowalski was scraping down the grill, singing something in Polish under his breath. He had owned the Blue Plate for thirty-one years, and in all that time, he liked to say, it had never once closed. Not for the blizzard of sixty-seven, not for the riots, not for his own wedding, which he had attended in the afternoon and then come back to work the night shift in his good suit. The doors had no lock. He had thrown the key in the river the day he bought the place, or so the story went.",
                "At a quarter past two the young man in the gray coat cleared his throat.",
                "\"Excuse me,\" he said. \"Is there a Rosa who works here?\"",
                "She set the coffeepot down. \"That's me.\"",
                "He looked at her for a long moment, as though checking her face against something he was carrying in his head. Then he reached into his coat and took out an envelope, soft and creased from being held, and laid it on the counter between them.",
                "\"My grandmother asked me to bring you this,\" he said. \"She said you'd know who it was from. She said to tell you she was sorry it took so long.\"",
                "Rosa did not pick it up. She could see the handwriting on the front, slanted and careful, the R made the old way with a little loop at the top, and for a moment the long bright room went very quiet around her, the way a room does when the power fails and every machine stops at once.",
                "Lenny had stopped eating. The nurse had put down her fork. Even Mr. Kowalski had stopped singing.",
                "\"Where is she?\" Rosa asked.",
                "\"Milwaukee,\" said the young man. \"She's been in Milwaukee the whole time.\"",
                "Outside, the rain kept coming down, and a bus went by on Halsted with its windows lit and empty, and the Blue Plate went on standing on its corner the way it always had, with the doors unlocked and the coffee on, waiting for whoever came in out of the night.",
            ),
        )

    /** The chapter every other book that is being read shares. */
    val Shared =
        DemoChapter(
            "Chapter One",
            listOf(
                "The road out of town ran straight for eleven miles before it bent around the grain elevator at Cedar Point, and in all that way there was nothing to look at but the fields and the fence posts and the long blue line of the hills to the west. Sam had driven it so many times that he could have done it with his eyes closed, and on some nights, coming home late from the hardware store, he very nearly had.",
                "But this morning he drove it slowly, with the window down and the radio off, because he knew it was the last time.",
                "The house was already empty. The furniture had gone on Tuesday in a truck from Omaha, and the boxes of books on Wednesday, and on Thursday he had walked from room to room with a broom, sweeping up the small things a family leaves behind without meaning to: a marble, a hair ribbon, a single playing card, the four of hearts, wedged behind the radiator in the front room where it must have been for twenty years.",
                "He had put the card in his shirt pocket. He did not know why.",
                "At the elevator he pulled over and got out and stood for a while beside the truck with his hands in his pockets. A meadowlark was singing somewhere out in the stubble. The sun was just coming up over the far edge of the county, turning the tin roof of the elevator the color of a new penny, and the air smelled of dust and cold and the faint, sweet rot of the last of the beets.",
                "His father had worked here, and his grandfather before that. There was a photograph of the two of them somewhere in one of the boxes, standing in front of this same building in their overalls, squinting into the same sun.",
                "Sam took the playing card out of his pocket and looked at it for a long time. Then he put it back, got into the truck, and drove on around the bend.",
            ),
        )

    /** The chapter book [id] opens to, or null for a book not yet begun. */
    fun forBook(id: Int): DemoChapter? =
        when (id) {
            DemoBooks.SaltRoad -> CottonExchange
            DemoBooks.MarshHollow -> FirstSnow
            DemoBooks.BluePlate -> LastCall
            else -> if (DemoBooks.reading.any { it.id == id }) Shared else null
        }
}
