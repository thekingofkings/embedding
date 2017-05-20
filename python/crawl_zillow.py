# -*- coding: utf-8 -*-
"""
Created on Sun Aug 28 15:18:12 2016

@author: gjz5038
"""

import requests
import numpy as np


def split_each_item(content):
    
    #split the item on each page (one page contains several houses)
    
    list_content = content.split('</div></article>')
    
    list_item = []
    for item in list_content:
        if item[:5] == '</li>':
            dict_item = convert_to_dict(item)
            list_item.append(dict_item)
        
    return list_item
    
def convert_to_dict(item_s):
        
    zpid = crawl_one_prop('zpid_', item_s) 
    # street = crawl_one_prop('streetAddress">', item_s)
    # city = crawl_one_prop('addressLocality">', item_s)
    # state = crawl_one_prop('addressRegion">', item_s)
    # zipcode = crawl_one_prop('postalCode" class="hide">', item_s)
    # price = crawl_one_prop('class="zsg-photo-card-price">$', item_s)
    # sold = crawl_one_prop('</span>SOLD: $', item_s)
    # sold_time = crawl_one_prop('Sold ', item_s)
    lat = crawl_one_prop('<meta itemprop="latitude" content="', item_s)
    lon = crawl_one_prop('<meta itemprop="longitude" content="', item_s)
    
    price_sqft = crawl_one_prop('Price/sqft: $', item_s)
    # bed_b = crawl_one_prop('"bed":', item_s)
    # bath_b = crawl_one_prop('"bath":', item_s)
    # sqft_b = crawl_one_prop('"sqft":', item_s)
    # 
    # bed_a = crawl_one_prop_backward(' bds', item_s)
    # bath_a = crawl_one_prop_backward(' ba', item_s)
    # sqft_a = crawl_one_prop_backward(' sqft', item_s)


    # bed = compare_and_choose(bed_a, bed_b)
    # bath = compare_and_choose(bath_a, bath_b)
    # sqft = compare_and_choose(sqft_a, sqft_b)
     
            
    return [zpid, lat, lon, price_sqft]
    # return [zpid, street, city, state, zipcode, price, sold, sold_time, price_sqft, lat, lon, bed, bath, sqft]
    
def compare_and_choose(a, b):
    if a == '-1' and b == '-1':
        return '-1'
    elif a == '-1':
        return b
    elif b == '-1':
        return a
    else:
        if float(a) != float(b):
            print ('reading error')
            raise ValueError
        else:
            return a     

def crawl_one_prop_backward(string_tail, item_s):
    
    pos = item_s.find(string_tail)
    if pos != -1:
        end = pos
        pos -= 1
        while item_s[pos] != '>' and item_s[pos] != '"' and item_s[pos] != ' ' : 
            pos -= 1
        value = item_s[pos+1:end]
    else:
        value = '-1'
       
    value = value.replace(',', '')
    value = value.replace('+', '')
    value = value.replace(';', '')
    if value == 'null' or value == '--' or value == '0':
        value = '-1'
    return value    
    
def crawl_one_prop(string_head, item_s):
    
    pos = item_s.find(string_head)
    if pos != -1:
        start = pos+len(string_head)
        for i in range(start, len(item_s)):
            if item_s[i] =='"' or item_s[i] == '<' or item_s[i] == '}':
                value = item_s[start:i]
                break
    else:
        value = '-1'
        
    value = value.replace(',', '')
    value = value.replace('+', '')
    value = value.replace(';', '')
    if value == 'null' or value == '--'  or value == '0':
        value = '-1'
    return value
    
        
def crawl(z, price_range, page):
    
    r = requests.get('http://www.zillow.com/homes/'+str(z)+'_rb/'+str(price_range[0])+'-'+str(price_range[1])+'_price/11_zm/1_rs/'+str(page)+'_p/')
    content = r.content
    list_item = split_each_item(str(content))
        
    return list_item


def main(file_name): 
    
    # 3 parameters you may want to change
    # n_page determines how many page you want to search in this zipcode and price range
    list_zip = range(60601, 60627) + range(60628, 60648) + [60649, 60651, 60652, 60653, 60655, 60656, 60657,
                                                            60659, 60660, 60661, 60666, 60667, 60827]
    assert len(list_zip) == 59

    list_price = [0, 100000, 200000, 300000, 400000, 500000, 600000, 700000, 800000, 900000, 1000000, 5000000]
    n_page = 20
    
    # array_data_head = ['zpid', 'street', 'city', 'state', 'zipcode',  'price', 'sold', 'soldTime', 'priceSqft', 'latitude', 'longitude', 'numBedrooms', 'numBathrooms', 'sqft']
    array_data_head = ['zpid', 'lat', 'lon', 'priceSqft']
    array_data = []
    for z in list_zip:
        for p in range(len(list_price)):
            if p == len(list_price) -1 :
                price_range = [list_price[p], 10000000]
            else:
                price_range = [list_price[p], list_price[p+1]]
                
            print ('zip: '+str(z)+',' +'price range: '+str(price_range[0])+'-'+str(price_range[1]))
            
            for page in range(n_page):
                
                print ('page: '+str(page))
                
                list_item = crawl(z, price_range, page)

                if len(list_item) == 0:
                    break
                
                array_data += list_item
    
    index_zpid = array_data_head.index('zpid')
    array_data = np.array(array_data)
    list_unique, unique_indecies = np.unique(array_data[:,index_zpid], return_index = True)
    
    
    # output the data
    
    print (len(array_data))
    
    array_data = array_data[unique_indecies]
    
    print (len(array_data))
    
    keep_list = []
    del_list = []
    list_must_have_attr = ['zpid','lat','lon','priceSqft']
    for i in range(len(array_data)):
        for attr in list_must_have_attr:
            index_attr =  array_data_head.index(attr)
            if array_data[i, index_attr] == '-1':
                del_list.append(i)
                break
    
    for ind in range(len(array_data)):
        if ind not in del_list:
            keep_list.append(ind)
            
    array_data = array_data[keep_list]
    
    for i in range(len(array_data)):
        for j in range(len(array_data[i])):
            array_data[i][j] = array_data[i][j].replace(',', '')
            
    f = open(file_name+'.csv', 'w')
    for i in range(len(array_data_head)):
        if i == len(array_data_head) -1:
            f.write(array_data_head[i]+'\n')
        else:
            f.write(array_data_head[i]+',')
            
    for r in range(len(array_data)):
        for i in range(len(array_data[r])):
            if i == len(array_data[r]) -1:
                f.write(array_data[r][i]+'\n')
            else:
                f.write(array_data[r][i]+',')
                
    f.close()


            
main('house_source_extra')


    
