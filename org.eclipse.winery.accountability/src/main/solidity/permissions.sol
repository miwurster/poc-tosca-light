/*******************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *******************************************************************************/
pragma solidity ^0.4.21;

contract Permissions {

    /**
     * taker => givers
     **/
    mapping(address => address[]) private givers;
    /**
     * taker => map(giver, permissions)
     **/
    mapping(address => mapping(address => bytes)) private permissions;

    function setPermission(address taker, bytes memory permissionPayload) public {
        permissions[taker][msg.sender] = permissionPayload;
        // always push! this can create duplicates which need to be checked by client application!
        givers[taker].push(msg.sender);
    }

    /**
     * Gets the permission payload that is given from a specific participant to the message sender.
     * 
     * returns the permission payload if the giver actually gave permission(s) to the message sender,
     * the array [] otherwise.
     **/
    function getPermission(address giver) public view returns (bytes memory){
        return permissions[msg.sender][giver];
    }

    /**
     * Gets the set of participant addresses which gave permissions to the message sender.
     * 
     * returns the set of permission givers if there are such givers. Otherwise, returns []. 
     * The "set" is not a real set as it can contain duplicates!
     **/
    function getGivers() public view returns (address[] memory) {
        return givers[msg.sender];
    }

}
