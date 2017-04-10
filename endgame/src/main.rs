#[macro_use]
extern crate lazy_static;

use std::cmp::max;

use std::fs::File;
use std::io::prelude::*;
use std::io::BufWriter;

/// CONFIGURATION_COUNT[n][m] represents the numbers of ways to put m beans into n pits.
/// It is used when converting a configuration into its index.
/// It is lazily initialized using dynamic programming.
lazy_static! {
    static ref CONFIGURATION_COUNT: [[u64; 73]; 13] = {
        let mut table = [[0u64; 73]; 13];

        for i in 0..73 {
            table[0][i] = 0;
            table[1][i] = 1;
        }
        //table[0][0] = 1;

        for i in 2..13 {
            for j in 0..73 {
                let mut tot: u64 = 0;
                for k in 0..j+1 {
                    tot += table[i-1][j-k];
                }
                table[i][j] = tot;
            }
        }
        
        table
    };
}

lazy_static! {
    /// Gives the number of even partition including the one with only zeros
    static ref EVEN_PARTITIONS: [u64; 37] = {
        let mut table = [0u64; 37];
        table[1] = CONFIGURATION_COUNT[12][2];

        for i in 2..37 {
            table[i] = table[i-1] + CONFIGURATION_COUNT[12][i*2];
        }

        table
    };
}

// TODO write documentation explanation
/// CONFIGURATION_SUM[i][j]
lazy_static! {
    static ref CONFIGURATION_SUM: [[u64; 73]; 13] = {
        let mut table = [[0u64; 73]; 13];

        for i in 0..13 {
            for j in (0..72).rev() {
                table[i][j] = table[i][j+1] + CONFIGURATION_COUNT[i][j];
            }
        }

        table
    };
}
                

/// Represents a possible configuration of the game ignoring the score each player has.
#[derive(Copy,Clone,Eq,PartialEq,Debug)]
struct Configuration {
    /// The number of beans in each of the pits. Pits 0-5 are the current player's and pits are
    /// placed in such a way that rotation clockwise is equivalent to adding 1 mod 12.
    pits: [u8; 12],

    /// How much skips the current player has
    player_remaining_skips: u8,

    /// How much skips the other player has
    opponent_remaining_skips: u8,

    cannot_be_skipped: bool
}

impl Configuration {
    /// Returns a configuration where current player and the other player are switched.
    fn switched(&self) -> Configuration {
        let mut new_pits: [u8; 12] = Default::default();
        for i in 0..6 {
            new_pits[i] = self.pits[6+i];
            new_pits[6+i] = self.pits[i];
        }
        Configuration { 
            pits: new_pits,
            player_remaining_skips: self.opponent_remaining_skips,
            opponent_remaining_skips: self.player_remaining_skips,
            cannot_be_skipped: self.cannot_be_skipped
        }
    }

    /// Simulates a sowing step. Returns the new configuration and the amount of beans that were
    /// seized.
    fn sow(&self, n: u8) -> (Configuration, u8) {
        let mut new = self.clone();
        new.cannot_be_skipped = false;

        let mut beans = new.pits[n as usize];
        new.pits[n as usize] = 0;

        let mut i = n as usize;
        while beans != 0 {
            i = (i+1) % 12;
            new.pits[i] += 1;
            beans -= 1;
        }

        let mut seized = 0;
        while new.pits[i] == 2 || new.pits[i] == 4 || new.pits[i] == 6 {
            seized += new.pits[i];
            new.pits[i] = 0;
            i += 12-1;
            i %= 12;
        }

        (new, seized)
    }

    fn skip(&self) -> Configuration {
        Configuration {
            player_remaining_skips: self.player_remaining_skips - 1,
            cannot_be_skipped: true,
            ..*self
        }
    }

    /// Number representing the remaining skips and ability to skip
    fn variation_number(&self) -> u8 {
        self.opponent_remaining_skips*5 + 
        match (self.player_remaining_skips, self.cannot_be_skipped) {
            (0,false) => 0,
            (0,true) => 1,
            (1,false) => 2,
            (1,true) => 3,
            (2,false) => 4,
            _ => unreachable!()
        }
    }


    fn to_index(&self) -> u64 {
        let partition_index = partition_to_index(&self.pits);

        partition_index*15 + self.variation_number() as u64
        //(sum, CONFIGURATION_COUNT[12][(sum as usize)*2]*mult + index)
    }
    
}



fn partition_to_index(pits: &[u8;12]) -> u64 {
    let sum = pits.iter().sum();

    if sum == 0 {
        return 0;
    }

    let mut remaining_sum: u8 = sum;

    let mut index: u64 = 0;

    for i in 0..12 {

        let next_remaining_sum: u8 = remaining_sum - pits[i];
        //println!("next_remaining_sum {} remaining_sum {}", next_remaining_sum, remaining_sum);
        let conrs = CONFIGURATION_SUM[12-i-1][(next_remaining_sum+1) as usize];
        let cors = CONFIGURATION_SUM[12-i-1][(remaining_sum+1) as usize];

        //println!("next_remaining_sum co {} remaining_sum co {}", conrs, cors);
        index += conrs-cors;
        remaining_sum = next_remaining_sum;
        //println!("{} {} {}", i, remaining_sum, index);
    }

    EVEN_PARTITIONS[((sum/2)-1) as usize] + index
}


#[derive(Copy,Clone,Eq,PartialEq,Debug)]
enum ConfigurationScore {
    /// The score
    Score(i8),
    /// The node wasn't visited yet
    NotVisited,
    /// The node was visited, but one of its children wasn't visited, so we placed it into a stack
    /// and postponed finding its score until the scores of its children are found.
    Postponed,
}

impl ConfigurationScore {
    fn pack(self) -> PackedConfigurationScore {
        use ConfigurationScore::*;
        PackedConfigurationScore(
            match self {
                Score(s) => s,
                NotVisited => 127,
                Postponed => 126,
            })
    }
}
            

#[derive(Copy,Clone,Eq,PartialEq)]
struct PackedConfigurationScore(i8);

impl PackedConfigurationScore {
    fn unpack(self) -> ConfigurationScore {
        use ConfigurationScore::*;
        match self {
            PackedConfigurationScore(127) => NotVisited,
            PackedConfigurationScore(126) => Postponed,
            PackedConfigurationScore(s) => Score(s),
        }
    }
}


/// Changes parition to the next partition and returns true if the last partition with that number
/// of values was obtained.
fn next_partition(array: &mut [u8]) -> bool {

    if array.len() == 1 {
        array[0] -= 1;
        true
    }
    else if array[1..].iter().sum::<u8>() == 0 {
        let len = array.len();
        array[len-1] = array[0]-1;
        array[0] = 0;
        true
    }
    else {
        if next_partition(&mut array[1..]) {
            array[0]+=1;
        }
        false
    }
}

fn get_simple_scores(scores: &mut Vec<PackedConfigurationScore>, config: &Configuration) -> PackedConfigurationScore {
    use ConfigurationScore::*;
    let index = config.to_index();
    let sum = config.pits.iter().sum::<u8>();
    if sum == 0 {
        return Score(0).pack();
    }
    if scores[index as usize] == NotVisited.pack() {
        if config.pits[6..12].iter().sum::<u8>() == 0 {
            scores[index as usize] = Score((sum) as i8).pack();
        }
    }
    scores[index as usize]
}



fn compute_score(scores: &mut Vec<PackedConfigurationScore>, config_: &Configuration) {
    use ConfigurationScore::*;
    if get_simple_scores(scores, config_) == NotVisited.pack() {
        let mut stack = Vec::new();
        stack.push(config_.clone());
        while let Some(config) = stack.pop() {
            //println!("{:?}", config);
            let index = config.to_index();
            let previously_got_score = scores[index as usize];
            match previously_got_score.unpack() {
                NotVisited | Postponed => {
                    let switched = config.switched();
                    let mut moves = Vec::with_capacity(7);
                    if switched.player_remaining_skips > 0 && !switched.cannot_be_skipped {
                        moves.push( (Configuration{
                            player_remaining_skips: switched.player_remaining_skips-1,
                            ..switched
                        },0) );
                    }
                    for i in 0..6u8 {
                        if (switched.pits[i as usize]!=0) {
                            moves.push(switched.sow(i));
                        }
                    }
                    let possible_scores = moves.iter().map(|&(new_config, beans_seized)| {
                        let score = get_simple_scores(scores, &new_config);
                        (new_config, match score.unpack() {
                            Score(s) => Score(s+(beans_seized as i8)).pack(),
                            something_else => something_else.pack(),
                        })
                    }).collect::<Vec<_>>();

                    /*
                            println!("Next moves are: ");
                            for &(conf,a) in &possible_scores {
                                println!(" {:?} ", (conf, a.unpack()) );
                            }
                            println!("");
                            */


                    let all_postponed = possible_scores.iter().all(|&(new_config, score)| {
                        score == Postponed.pack()
                    });
                    if all_postponed {
                        if previously_got_score == Postponed.pack() {
                            panic!("At least some of the postponed values must have been found");
                        }
                        continue;
                    }

                    let total_score = possible_scores.iter().fold(None, |acc, &(new_config, score)| {
                        match (score.unpack(),acc) {
                            (Score(a), Some(b)) => Some(max(a,b)),
                            (Score(a), None) => Some(a),
                            _ => acc
                        }
                    });

                    if (previously_got_score == Postponed.pack()) {
                        if let Some(the_score) = total_score {
                            scores[index as usize] = Score(-the_score).pack();
                        }
                        else {
                            scores[index as usize] = NotVisited.pack();
                        }
                    }
                    else {

                        let exist_not_visited = possible_scores.iter().any(|&(new_config, score)| {
                            score == NotVisited.pack()
                        });

                        if (exist_not_visited) {
                            scores[index as usize] = Postponed.pack();
                            stack.push(config);
                            for (new_config,score) in possible_scores {
                                if score == NotVisited.pack() {
                                    stack.push(new_config);
                                }
                            }
                        }
                        else {
                            scores[index as usize] = Score(-total_score.unwrap()).pack();
                        }
                    }

                },
                _ => {}
            }
        }
    }
}

fn main() {

    let num = 4u8;

    let mut scores: Vec<PackedConfigurationScore> = vec![ConfigurationScore::NotVisited.pack(); (EVEN_PARTITIONS[num as usize]*15) as usize];

    let mut file = File::create(format!("uncompressed_endgames_since_beginning_till_{}",num*2)).unwrap();
    let mut file = BufWriter::new(file);


    let mut count: u64 = 0;

    'outer:
    for ii in 1..(num+1) {
        let i = ii*2;

        println!("{}", i);
        println!("{}", CONFIGURATION_COUNT[12][i as usize] *16);

        //scores.push(vec![ConfigurationScore::NotVisited.pack(); (CONFIGURATION_COUNT[12][i as usize]*16) as usize]);
        let mut pits = [0,0,0,0,0,0,0,0,0,0,0,i];

        let variations = [(0,false), (0,true), (1,false), (1,true), (2,false)];

        loop {
            for opponent_remaining_skips in 0..3 {
                for variation in variations.iter() {
                    let config = Configuration{
                        pits: pits,
                        player_remaining_skips: variation.0,
                        opponent_remaining_skips: opponent_remaining_skips,
                        cannot_be_skipped: variation.1,
                    };

                    //assert!(config.to_index() == count);

                    compute_score(&mut scores, &config);
                    //println!("{:?} -> {:?}",config, s.unpack());
                    //println!("The config {:?} has index {:?}", config, config.to_index());
                    //println!("{}", count);
                    //count += 1;
                }
            }
            if next_partition(&mut pits) {
                break;
            }
        }
        println!("Now righting to file {}", i);

        let start = (EVEN_PARTITIONS[(ii-1) as usize]*15) as usize;
        let end = (EVEN_PARTITIONS[ii as usize]*15) as usize;
        
        for score in scores[start..end].iter() {

            if let ConfigurationScore::Score(the_score) = score.unpack() {
                let buffer: [u8; 1] = [the_score as u8];
                file.write_all(&buffer).unwrap();
            }
            else {
                panic!("This should all be filled out.");
            }
        }

        println!("Done righting to file {}", i);


    }
    

    /*
    println!("Will start writing to file.");


    for (ii,a) in scores.iter().enumerate() {
        let i = ii*2;
        println!("{} {}", i, a.len() );
        for &score in a{
            
            if let ConfigurationScore::Score(the_score) = score.unpack() {
                let buffer: [u8; 1] = [the_score as u8];
                file.write_all(&buffer).unwrap();
            }
            else {
                panic!("This should all be filled out.");
            }
        }
    }
    */

}
